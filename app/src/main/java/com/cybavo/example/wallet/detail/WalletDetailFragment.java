/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cybavo.example.wallet.NavFragment;
import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.CurrencyHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.example.wallet.main.MainViewModel;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Transaction;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.RenameWalletResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class WalletDetailFragment extends Fragment implements RenameWalletDialog.OnRenameListener, HistoryListAdapter.HistoryListListener {

    private static final String ARG_WALLET = "wallet";

    private Wallet mWallet;
    private MainViewModel mViewModel;
    private DetailViewModel mDetailViewModel;

    private Toolbar mToolbar;

    private TextView mBalance;
    private TextView mBalanceUnconfirmed;
    private TextView mCurrency;
    private TextView mAddress;
    private Button mDeposit;
    private Button mWithdraw;

    private MaterialButtonToggleGroup mTime;
    private MaterialButtonToggleGroup mDirection;
    private MaterialButtonToggleGroup mPending;
    private MaterialButtonToggleGroup mSuccess;
    private View mMoreFilters;

    private SwipeRefreshLayout mSwipeRefresh;
    private RecyclerView mHistoryList;
    private HistoryListAdapter mHistoryAdapter;

    private View mLoading;
    private TextView mLoadingText;
    private ProgressBar mLoadingProgress;


    final MaterialButtonToggleGroup.OnButtonCheckedListener SINGLE_SELECTION_HACK = (group, checkedId, isChecked) -> {
        if (!isChecked && group.getCheckedButtonIds().size() == 0) {
            ((MaterialButton) group.findViewById(checkedId)).setChecked(true);
            return;
        }
    };

    public WalletDetailFragment() {
        // Required empty public constructor
    }

    public static WalletDetailFragment newInstance(Wallet wallet) {
        WalletDetailFragment fragment = new WalletDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLET, WalletParcelable.fromWallet(wallet));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWallet = WalletParcelable.toWallet(getArguments().getParcelable(ARG_WALLET));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wallet_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(mWallet.name.isEmpty() ? getString(R.string.label_unnamed_wallet) : mWallet.name)
                .menu(R.menu.detail, item -> {
                    switch (item.getItemId()) {
                        case R.id.action_rename:
                            rename();
                            return true;
                        case R.id.action_eos_resources:
                            goEOSResource();
                            return true;
                        default:
                            return false;
                    }
                })
                .onBack(v -> getFragmentManager().popBackStack())
                .done();

        // set EOS resources menu visible
        mToolbar.getMenu().findItem(R.id.action_eos_resources)
                .setVisible(mWallet.currency == CurrencyHelper.Coin.EOS);

        mBalance = view.findViewById(R.id.balance);
        mBalanceUnconfirmed = view.findViewById(R.id.balanceUnconfirmed);
        mCurrency = view.findViewById(R.id.currency);
        mAddress = view.findViewById(R.id.address);

        mCurrency.setText(mWallet.currencySymbol);
        mAddress.setText(mWallet.address);

        mDeposit = view.findViewById(R.id.deposit);
        mDeposit.setOnClickListener(v -> {
            deposit();
        });

        mWithdraw = view.findViewById(R.id.withdraw);
        mWithdraw.setOnClickListener(v -> {
            withdraw();
        });

        mLoading = view.findViewById(R.id.loading);
        mLoadingText = view.findViewById(R.id.loadingText);
        mLoadingProgress = view.findViewById(R.id.loadingProgress);
        DrawableCompat.setTint(mLoadingProgress.getIndeterminateDrawable(), Color.WHITE);

        mHistoryList = view.findViewById(R.id.history);
        mHistoryAdapter = new HistoryListAdapter(mWallet, this);
        mHistoryList.setAdapter(mHistoryAdapter);
        final LinearLayoutManager llm = new LinearLayoutManager(getContext());
        mHistoryList.setLayoutManager(llm);
        mHistoryList.addOnScrollListener(new EndlessScrollListener(llm) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                mDetailViewModel.fetchHistory(totalItemsCount);
            }
        });

        mSwipeRefresh = view.findViewById(R.id.swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(() -> {
            refresh();
        });

        mTime = view.findViewById(R.id.time);
        mDirection = view.findViewById(R.id.direction);
        mPending = view.findViewById(R.id.pending);
        mSuccess = view.findViewById(R.id.success);

        mMoreFilters = view.findViewById(R.id.moreFilters);
        mMoreFilters.setOnClickListener(v -> {
            mMoreFilters.setVisibility(View.GONE);
            mTime.setVisibility(View.VISIBLE);
            mPending.setVisibility(View.VISIBLE);
            mSuccess.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // global ViewModel
        mViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);

        mViewModel.getBalance(mWallet).observe(this, entry -> {
            if (!entry.init) {
                mBalance.setText("…");
            } else {
                mBalance.setText(CurrencyHelper.getEffectiveBalance(entry.balance));
                // unconfirmed balance
                if (mWallet.tokenAddress.isEmpty() && // only valid for native token
                        !entry.balance.unconfirmedBalance.isEmpty() && !"0".equals(entry.balance.unconfirmedBalance)) { // unconfirmed balance is present
                    mBalanceUnconfirmed.setText(entry.balance.unconfirmedBalance);
                }
            }
        });

        // detail ViewModel
        mDetailViewModel = ViewModelProviders.of(this,
                new DetailViewModel.Factory(getActivity().getApplication(), mWallet))
                .get(DetailViewModel.class);
        mDetailViewModel.getHistory().observe(this, history -> {
            if (!history.init) { // initializing
                mLoading.setVisibility(View.GONE);
                mSwipeRefresh.setRefreshing(true);
            } else if (history.loading) { // loading more
                mLoading.setVisibility(View.VISIBLE);
                mLoadingProgress.setVisibility(View.VISIBLE);
                mLoadingText.setText(R.string.label_loading_more);
                mSwipeRefresh.setRefreshing(false);
            } else if (!history.hasMore) { // no more
                mLoading.setVisibility(View.VISIBLE);
                mLoadingProgress.setVisibility(View.GONE);
                mLoadingText.setText(R.string.label_no_more);
                mSwipeRefresh.setRefreshing(false);
            } else { // idle
                mLoading.setVisibility(View.GONE);
                mSwipeRefresh.setRefreshing(false);
            }
            mHistoryAdapter.updateTransactions(history.history);
        });

        mTime.addOnButtonCheckedListener(SINGLE_SELECTION_HACK);
        mTime.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            final Calendar calNow = Calendar.getInstance();
            final Calendar calMidnight = Calendar.getInstance();
            calMidnight.set(Calendar.HOUR_OF_DAY, 0);
            calMidnight.set(Calendar.MINUTE, 0);
            calMidnight.set(Calendar.SECOND, 0);
            calMidnight.set(Calendar.MILLISECOND, 0);

            final long now = calNow.getTimeInMillis() / 1000;
            final long midnight = calMidnight.getTimeInMillis() / 1000;

            if (isChecked) {
                switch (checkedId) {
                    case R.id.timeAll:
                        mDetailViewModel.setTime(null);
                        break;
                    case R.id.timeToday:
                        mDetailViewModel.setTime(Pair.create(midnight, now));
                        break;
                    case R.id.timeYesterday:
                        mDetailViewModel.setTime(Pair.create(midnight - 86400, midnight));
                        break;
                }
            }
        });

        mDirection.addOnButtonCheckedListener(SINGLE_SELECTION_HACK);
        mDirection.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                switch (checkedId) {
                    case R.id.directionAll:
                        mDetailViewModel.setDirectionFilter(null);
                        break;
                    case R.id.directionIn:
                        mDetailViewModel.setDirectionFilter(Transaction.Direction.IN);
                        break;
                    case R.id.directionOut:
                        mDetailViewModel.setDirectionFilter(Transaction.Direction.OUT);
                        break;
                }
            }
        });

        mPending.addOnButtonCheckedListener(SINGLE_SELECTION_HACK);
        mPending.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                switch (checkedId) {
                    case R.id.pendingAll:
                        mDetailViewModel.setPendingFilter(null);
                        break;
                    case R.id.pendingTrue:
                        mDetailViewModel.setPendingFilter(true);
                        break;
                    case R.id.pendingFalse:
                        mDetailViewModel.setPendingFilter(false);
                        break;
                }
            }
        });

        mSuccess.addOnButtonCheckedListener(SINGLE_SELECTION_HACK);
        mSuccess.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                switch (checkedId) {
                    case R.id.successAll:
                        mDetailViewModel.setSuccessFilter(null);
                        break;
                    case R.id.successTrue:
                        mDetailViewModel.setSuccessFilter(true);
                        break;
                    case R.id.successFalse:
                        mDetailViewModel.setSuccessFilter(false);
                        break;
                }
            }
        });
    }

    @Override
    public void onRename(String name) {
        Wallets.getInstance().renameWallet(mWallet.walletId, name, new Callback<RenameWalletResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getContext(), "renameWallet failed: " + error.getMessage());
            }

            @Override
            public void onResult(RenameWalletResult result) {
                mWallet.name = name;
                mToolbar.setTitle(name);
                mViewModel.fetchWallets(); // refresh list
            }
        });
    }

    public void refresh() {
        Wallets.getInstance().getWallet(mWallet.walletId, null);
        mDetailViewModel.refreshHistory();
        mViewModel.getBalance(mWallet, true);
    }

    private void rename() {
        final String tag = RenameWalletDialog.class.getSimpleName();
        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            RenameWalletDialog.newInstance(mWallet)
                    .show(getChildFragmentManager(), tag);
        }
    }

    private void deposit() {
        NavFragment.find(this).goDeposit(mWallet);
    }

    private void withdraw() {
        NavFragment.find(this).goWithdraw(mWallet);
    }

    private void goEOSResource() {
        NavFragment.find(this).goEOSResource(mWallet);
    }

    @Override
    public void onClickTransaction(Transaction item) {
        NavFragment.find(this).goTransactionDetail(mWallet, item);
    }
}

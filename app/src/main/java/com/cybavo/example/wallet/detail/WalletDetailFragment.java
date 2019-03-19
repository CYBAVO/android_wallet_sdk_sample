package com.cybavo.example.wallet.detail;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.cybavo.wallet.service.wallet.results.ClearSecureTokenResult;
import com.cybavo.wallet.service.wallet.results.RenameWalletResult;

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
    private TextView mCurrency;
    private TextView mAddress;
    private Button mDeposit;
    private Button mWithdraw;

    private SwipeRefreshLayout mSwipeRefresh;
    private RecyclerView mHistoryList;
    private HistoryListAdapter mHistoryAdapter;

    private View mLoading;
    private TextView mLoadingText;
    private ProgressBar mLoadingProgress;

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
                    if (item.getItemId() == R.id.action_rename) {
                        rename();
                        return true;
                    }
                    return false;
                })
                .onBack(v -> getFragmentManager().popBackStack())
                .done();

        mBalance = view.findViewById(R.id.balance);
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
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // global ViewModel
        mViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);

        mViewModel.getBalance(mWallet).observe(this, entry -> {
            mBalance.setText(entry.init ? CurrencyHelper.getEffectiveBalance(entry.balance) : "…");
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
    }

    @Override
    public void onStop() {
        super.onStop();
        Wallets.getInstance().clearSecureToken(new Callback<ClearSecureTokenResult>() {
            @Override
            public void onError(Throwable error) {
                mViewModel.getBalance(mWallet, true);
                Helpers.showToast(getContext(), "clearSecureToken failed: " + error.getMessage());
            }

            @Override
            public void onResult(ClearSecureTokenResult clearSecureTokenResult) {
                Helpers.showToast(getContext(), "Secure Token revoked");
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

    @Override
    public void onClickTransaction(Transaction item) {
        NavFragment.find(this).goTransactionDetail(mWallet, item);
    }
}

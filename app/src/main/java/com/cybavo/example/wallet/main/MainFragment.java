/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cybavo.example.wallet.NavFragment;
import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.ClearSecureTokenResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainFragment extends Fragment implements WalletListAdapter.WalletListListener {

    public MainFragment() {
    }

    private MainViewModel mViewModel;
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefresh;
    private WalletListAdapter mAdapter;
    private RecyclerView mWalletList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwipeRefresh = view.findViewById(R.id.swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(() -> refresh());

        mWalletList = view.findViewById(R.id.list);
        mWalletList.setLayoutManager(new LinearLayoutManager(getContext()));

        mToolbar = ToolbarHelper.setupToolbar(view, R.id.appBar).noBack()
                .title(R.string.title_wallet)
                .menu(R.menu.main, item -> {
                    switch (item.getItemId()) {
                        case R.id.action_create_wallet:
                            createWallet();
                            return true;
                        case R.id.action_settings:
                            goSettings();
                            return true;
                    }
                    return false;
                })
                .done();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);

        mViewModel.getUserState().observe(this, userState -> {
            if (userState != null) {
                if (userState.setPin) {
                    refresh();
                } else {
                    setupPin();
                }
            }

        });

        mAdapter = new WalletListAdapter(this, mViewModel, this);
        mWalletList.setAdapter(mAdapter);

        mViewModel.getWallets().observe(this, wallets -> {
            mAdapter.updateWallets(wallets);
        });
        mViewModel.getLoadingWallets().observe(this, loading -> {
            mSwipeRefresh.setRefreshing(loading);
        });
        mViewModel.getCreatableCurrencies().observe(this, currencies -> {
            MenuItem item = mToolbar.getMenu().findItem(R.id.action_create_wallet);
            if (item != null) {
                item.setVisible(!currencies.isEmpty());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearSecureToken();
    }

    @Override
    public void onClickWallet(Wallet wallet) {
        NavFragment.find(this).goWalletDetail(wallet);
    }

    private void refresh() {
        mViewModel.fetchWallets();
    }
    
    private void setupPin() {
        NavFragment.find(this).goSetup();
    }

    private void createWallet() {
        NavFragment.find(this).goCreateWallet();
    }

    private void goSettings() {
        NavFragment.find(this).goSettings();
    }

    private void clearSecureToken() {
        final Context context = getContext().getApplicationContext();
        Wallets.getInstance().clearSecureToken(new Callback<ClearSecureTokenResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(context, "clearSecureToken failed: " + error.getMessage());
            }

            @Override
            public void onResult(ClearSecureTokenResult clearSecureTokenResult) {
                Helpers.showToast(context, "Secure Token revoked");
            }
        });
    }
}

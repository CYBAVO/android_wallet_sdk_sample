/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;

import android.app.Application;

import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Fee;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.GetTransactionFeeResult;
import com.cybavo.wallet.service.wallet.results.GetWalletUsageResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class WithdrawViewModel extends AndroidViewModel {

    private static final String TAG = WithdrawViewModel.class.getSimpleName();

    public static class Factory implements ViewModelProvider.Factory {

        private final Application mApp;
        private final Wallet mWallet;

        public Factory(Application application, Wallet wallet) {
            mApp = application;
            mWallet = wallet;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new WithdrawViewModel(mApp, mWallet);
        }
    }

    private Wallets mService = Wallets.getInstance();
    private final Wallet mWallet;

    private LiveData<GetWalletUsageResult> mUsage;
    private MutableLiveData<List<Fee>> mFee;

    public WithdrawViewModel(@NonNull Application application, Wallet wallet) {
        super(application);
        mWallet = wallet;
    }

    public @NonNull LiveData<List<Fee>> getTransactionFee() {
        if (mFee == null) {
            mFee = new MutableLiveData<>();
            mFee.setValue(new ArrayList<>());
            fetchTransactionFee();
        }
        return mFee;
    }

    public LiveData<GetWalletUsageResult> getUsage() {
        if (mUsage == null) {
            MutableLiveData<GetWalletUsageResult> result = new MutableLiveData<>();
            fetchUsage(mWallet, result);
            mUsage = result;
        }
        return mUsage;
    }

    private void fetchTransactionFee() {
        mService.getTransactionFee(mWallet.currency, new Callback<GetTransactionFeeResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getApplication(), "getTransactionFee failed: " + error.getMessage());
            }

            @Override
            public void onResult(GetTransactionFeeResult result) {
                mFee.setValue(Arrays.asList(result.low, result.medium, result.high));
            }
        });
    }

    private void fetchUsage(Wallet wallet, MutableLiveData<GetWalletUsageResult> entry) {
        mService.getWalletUsage(wallet.walletId, new Callback<GetWalletUsageResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getApplication(), "getWalletUsage failed: " + error.getMessage());
            }

            @Override
            public void onResult(GetWalletUsageResult result) {
                entry.setValue(result);
            }
        });
    }
}

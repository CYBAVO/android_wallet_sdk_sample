/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.create;

import android.app.Application;

import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Wallet;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CreateWalletViewModel extends AndroidViewModel {

    private static final String TAG = CreateWalletViewModel.class.getSimpleName();

    public static class Factory implements ViewModelProvider.Factory {

        private final Application mApp;
        private final LiveData<List<Wallet>> mWallets;

        public Factory(Application application, LiveData<List<Wallet>> wallets) {
            mApp = application;
            mWallets = wallets;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new CreateWalletViewModel(mApp, mWallets);
        }
    }

    private final MutableLiveData<Currency> mSelectedCurrency;
    private final MutableLiveData<Long> mSelectedParent;
    private final MediatorLiveData<List<Wallet>> mAvailableParents;

    public CreateWalletViewModel(@NonNull Application application, LiveData<List<Wallet>> wallets) {
        super(application);

        mSelectedCurrency = new MutableLiveData<>();
        mSelectedCurrency.setValue(Currency.Unknown);

        mSelectedParent = new MutableLiveData<>();
        mSelectedParent.setValue(0L);

        mAvailableParents = new MediatorLiveData<>();
        mAvailableParents.setValue(new ArrayList<>());
        mAvailableParents.addSource(wallets, walletList -> {
            mAvailableParents.setValue(calcParentWallets(walletList, mSelectedCurrency.getValue()));
        });
        mAvailableParents.addSource(mSelectedCurrency, currency -> {
            mAvailableParents.setValue(calcParentWallets(wallets.getValue(), currency));
        });
    }

    public LiveData<Currency> getSelectedCurrency() {
        return mSelectedCurrency;
    }

    public void setSelectedCurrency(Currency currency) {
        mSelectedCurrency.setValue(currency);
    }

    public LiveData<Long> getSelectedParent() {
        return mSelectedParent;
    }

    public void setSelectedParent(long walletId) {
        mSelectedParent.setValue(walletId);
    }

    public LiveData<List<Wallet>> getAvailableParents() {
        return mAvailableParents;
    }

    private List<Wallet> calcParentWallets(List<Wallet> wallets, Currency currency) {
        List<Wallet> parents = new ArrayList<>();
        for (Wallet w : wallets) {
            if (currency.currency ==  w.currency && w.tokenAddress.isEmpty()) {
                parents.add(w);
            }
        }
        return parents;
    }
}

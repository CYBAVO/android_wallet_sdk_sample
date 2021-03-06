/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.main;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.cybavo.example.wallet.auth.Identity;
import com.cybavo.example.wallet.config.Config;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.UserState;
import com.cybavo.wallet.service.auth.results.GetUserStateResult;
import com.cybavo.wallet.service.wallet.Balance;
import com.cybavo.wallet.service.wallet.BalanceAddress;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.GetBalancesResult;
import com.cybavo.wallet.service.wallet.results.GetCurrenciesResult;
import com.cybavo.wallet.service.wallet.results.GetWalletsResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class MainViewModel extends AndroidViewModel {

    private static final String TAG = MainViewModel.class.getSimpleName();

    public static class Factory implements ViewModelProvider.Factory {

        private final Application mApp;

        public Factory(Application application) {
            mApp = application;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new MainViewModel(mApp);
        }
    }

    private final static long BALANCE_THROTTLE = 10 * 1000; // throttle in 10 seconds

    private Wallets mService = Wallets.getInstance();

    private MutableLiveData<UserState> mUserState;
    private MutableLiveData<List<Wallet>> mWallets;
    private MutableLiveData<Boolean> mLoadingWallets = new MutableLiveData<>();
    private MutableLiveData<List<Currency>> mCurrencies;
    private MediatorLiveData<List<Currency>> mCreatableCurrencies;

    private Map<BalanceKey, MutableLiveData<BalanceEntry>> mBalances = new HashMap<>();

    private Identity mIdentity;

    public MainViewModel(@NonNull Application application) {
        super(application);
        mLoadingWallets.setValue(false);
        mIdentity = Identity.read(application);
    }

    public LiveData<List<Wallet>> getWallets() {
        if (mWallets == null) {
            mWallets = new MutableLiveData<>();
            mWallets.setValue(new ArrayList<>());
            fetchWallets();
        }
        return mWallets;
    }

    public void fetchWallets() {
        if (mWallets == null) {
            mWallets = new MutableLiveData<>();
            mWallets.setValue(new ArrayList<>());
        }
        if (mLoadingWallets.getValue()) { // already loading
            return;
        }
        mLoadingWallets.setValue(true);
        mService.getWallets(new Callback<GetWalletsResult>() {
            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "getWallets failed", error);
                Helpers.showToast(getApplication(), "fetchWallets failed: " + error.getMessage());
                mLoadingWallets.setValue(false);
            }

            @Override
            public void onResult(GetWalletsResult result) {
                mWallets.setValue(Arrays.asList(result.wallets));
                mLoadingWallets.setValue(false);
            }
        });
    }

    public LiveData<Boolean> getLoadingWallets() {
        return mLoadingWallets;
    }

    public LiveData<List<Currency>> getCurrencies() {
        if (mCurrencies == null) {
            mCurrencies = new MutableLiveData<>();
            mCurrencies.setValue(new ArrayList<>());
            fetchCurrencies();
        }
        return mCurrencies;
    }

    private void fetchCurrencies() {
        mService.getCurrencies(new Callback<GetCurrenciesResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getApplication(), "fetchCurrencies failed: " + error.getMessage());
            }

            @Override
            public void onResult(GetCurrenciesResult result) {
                mCurrencies.setValue(Arrays.asList(result.currencies));
            }
        });
    }

    public @NonNull LiveData<BalanceEntry> getBalance(Wallet wallet) {
        return getBalance(wallet, false);
    }

    public @NonNull LiveData<BalanceEntry> getBalance(Wallet wallet, boolean refresh) {
        final BalanceKey key = new BalanceKey(wallet.currency, wallet.tokenAddress, wallet.address);
        MutableLiveData<BalanceEntry> entry = mBalances.get(key);
        if (entry == null) {
            entry = new MutableLiveData<>();
            entry.setValue(new BalanceEntry(new Balance(), 0, false));
            mBalances.put(key, entry);
        }

        if (refresh || SystemClock.uptimeMillis() - entry.getValue().updatedAt > BALANCE_THROTTLE) { // skip in throttle
            fetchBalance(wallet);
        }
        return entry;
    }

    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private FetchBalanceBatch mFetchBalanceBatch;
    class FetchBalanceBatch implements Runnable {

        final private List<BalanceAddress> mBatch = new ArrayList<>();

        void add(Wallet wallet) {
            // skip exists
            for (BalanceAddress address : mBatch) {
                if (address.currency == wallet.currency &&
                        address.tokenAddress.equals(wallet.tokenAddress) &&
                        address.address.equals(wallet.address)) {
                    return;
                }
            }

            final BalanceAddress address = new BalanceAddress();
            address.currency = wallet.currency;
            address.tokenAddress = wallet.tokenAddress;
            address.address = wallet.address;
            mBatch.add(address);
        }

        @Override
        public void run() {
            mFetchBalanceBatch = null;

            final Map<Integer, BalanceAddress> request = new HashMap<>();
            for (int i = 0; i < mBatch.size(); i++) {
                request.put(i, mBatch.get(i));
            }

            mService.getBalances(request, new Callback<GetBalancesResult>() {
                @Override
                public void onError(Throwable error) {
                    Helpers.showToast(getApplication(), "getBalance failed: " + error.getMessage());
                }

                @Override
                public void onResult(GetBalancesResult result) {
                    for (Map.Entry<Integer, Balance> balance : result.balances.entrySet()) {
                        final Integer key = balance.getKey();
                        final Balance value = balance.getValue();
                        final BalanceAddress address = request.get(key);

                        MutableLiveData<BalanceEntry> entry = mBalances.get(
                                new BalanceKey(address.currency, address.tokenAddress, address.address));
                        if (entry != null) {
                            entry.setValue(new BalanceEntry(value, SystemClock.uptimeMillis(), true));
                        }
                    }
                }
            });
        }
    }

    private void fetchBalance(Wallet wallet) {

        if (mFetchBalanceBatch == null) {
            mFetchBalanceBatch = new FetchBalanceBatch();
            mMainHandler.postDelayed(mFetchBalanceBatch, 500);
        }

        mFetchBalanceBatch.add(wallet);
    }

    public LiveData<UserState> getUserState() {
        if (mUserState == null) {
            mUserState = new MutableLiveData<>();
            fetchUserState();
        }
        return mUserState;
    }

    public void fetchUserState() {
        Auth.getInstance().getUserState(new Callback<GetUserStateResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getApplication(), "getUserState failed: " + error.getMessage());
            }

            @Override
            public void onResult(GetUserStateResult result) {
                mIdentity.email = result.userState.email;
                mIdentity.name = result.userState.realName;
                mIdentity.save(getApplication());
                mUserState.setValue(result.userState);
            }
        });
    }

    public LiveData<List<Currency>> getCreatableCurrencies() {
        if (mCreatableCurrencies == null) {
            mCreatableCurrencies = new MediatorLiveData<>();
            Observer<Object> observer = x -> {
                List<Currency> allCurrencies = getCurrencies().getValue();
                List<Wallet> allWallets = getWallets().getValue();
                List<Currency> available = new ArrayList<>();
                for (Currency c : allCurrencies) {
                    if (Config.canCreateWallet(allWallets, c)) {
                        available.add(c);
                    }
                }
                mCreatableCurrencies.setValue(available);
            };
            mCreatableCurrencies.addSource(getCurrencies(), observer);
            mCreatableCurrencies.addSource(getWallets(), observer);
        }
        return mCreatableCurrencies;
    }
}

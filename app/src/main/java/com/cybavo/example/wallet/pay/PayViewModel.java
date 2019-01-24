package com.cybavo.example.wallet.pay;

import android.app.Application;
import android.os.SystemClock;
import android.util.Log;

import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.main.BalanceEntry;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Balance;
import com.cybavo.wallet.service.wallet.BalanceAddress;
import com.cybavo.wallet.service.wallet.Fee;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.GetBalancesResult;
import com.cybavo.wallet.service.wallet.results.GetTransactionFeeResult;
import com.cybavo.wallet.service.wallet.results.GetWalletUsageResult;
import com.cybavo.wallet.service.wallet.results.GetWalletsResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class PayViewModel extends AndroidViewModel {

    private static final String TAG = PayViewModel.class.getSimpleName();

    public static class Factory implements ViewModelProvider.Factory {

        private final Application mApp;
        private final int mCurrency;
        private final String mTokenAddress;

        public Factory(Application application, int currency, String tokenAddress) {
            mApp = application;
            mCurrency = currency;
            mTokenAddress = tokenAddress;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new PayViewModel(mApp, mCurrency, mTokenAddress);
        }
    }

    private Wallets mService = Wallets.getInstance();
    private final int mCurrency;
    private final String mTokenAddress;

    private MutableLiveData<List<Wallet>> mAvailableWallets;

    private MutableLiveData<Wallet> mSelectedWallet = new MutableLiveData<>();
    private LiveData<GetWalletUsageResult> mUsage;
    private LiveData<BalanceEntry> mBalance;
    private MutableLiveData<List<Fee>> mFee;

    public PayViewModel(@NonNull Application application, int currency, String tokenAddress) {
        super(application);
        mCurrency = currency;
        mTokenAddress = tokenAddress;

        mBalance = Transformations.switchMap(mSelectedWallet, wallet -> {
            MutableLiveData<BalanceEntry> entry = new MutableLiveData<>();
            entry.setValue(new BalanceEntry(new Balance(), 0, false));
            fetchBalance(wallet, entry);
            return entry;
        });

        mUsage = Transformations.switchMap(mSelectedWallet, wallet -> {
            MutableLiveData<GetWalletUsageResult> result = new MutableLiveData<>();
            fetchUsage(wallet, result);
            return result;
        });
    }

    public LiveData<List<Wallet>> getAvailableWallets() {
        if (mAvailableWallets == null) {
            mAvailableWallets = new MutableLiveData<>();
            mAvailableWallets.setValue(new ArrayList<>());
            fetchWallets();
        }
        return mAvailableWallets;
    }

    public void fetchWallets() {
        if (mAvailableWallets == null) {
            mAvailableWallets = new MutableLiveData<>();
            mAvailableWallets.setValue(new ArrayList<>());
        }
        mService.getWallets(new Callback<GetWalletsResult>() {
            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "getWallets failed", error);
                Helpers.showToast(getApplication(), "fetchWallets failed: " + error.getMessage());
            }

            @Override
            public void onResult(GetWalletsResult result) {
                List<Wallet> wallets = new ArrayList<>();
                for (final Wallet w : result.wallets) {
                    if (w.currency == mCurrency && w.tokenAddress.equals(mTokenAddress)) {
                        wallets.add(w);
                    }
                }
                mAvailableWallets.setValue(wallets);
            }
        });
    }

    public void setSelectedWallet(Wallet wallet) {
        mSelectedWallet.setValue(wallet);
    }

    public LiveData<Wallet> getSelectedWallet() {
        return mSelectedWallet;
    }

    public @NonNull LiveData<List<Fee>> getTransactionFee() {
        if (mFee == null) {
            mFee = new MutableLiveData<>();
            mFee.setValue(new ArrayList<>());
            fetchTransactionFee();
        }
        return mFee;
    }

    public LiveData<BalanceEntry> getBalance() {
        return mBalance;
    }

    public LiveData<GetWalletUsageResult> getUsage() {
        return mUsage;
    }

    private void fetchTransactionFee() {
        mService.getTransactionFee(mCurrency, new Callback<GetTransactionFeeResult>() {
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

    private void fetchBalance(Wallet wallet, MutableLiveData<BalanceEntry> entry) {

        final Map<Integer, BalanceAddress> addresses = new HashMap<>();
        final BalanceAddress address = new BalanceAddress();
        address.currency = wallet.currency;
        address.tokenAddress = wallet.tokenAddress;
        address.address = wallet.address;
        addresses.put(0, address);

        mService.getBalances(addresses, new Callback<GetBalancesResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getApplication(), "getBalance failed: " + error.getMessage());
            }

            @Override
            public void onResult(GetBalancesResult result) {
                if (result.balance.get(0) != null) {
                    entry.setValue(new BalanceEntry(result.balance.get(0), SystemClock.uptimeMillis(), true));
                }
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

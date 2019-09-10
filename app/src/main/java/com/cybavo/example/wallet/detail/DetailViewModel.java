/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;

import android.app.Application;
import android.util.Log;
import android.util.Pair;

import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Transaction;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.GetHistoryResult;
import com.cybavo.wallet.service.wallet.results.GetTransactionInfoResult;
import com.cybavo.wallet.service.wallet.results.GetTransactionsInfoResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class DetailViewModel extends AndroidViewModel {

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
            return (T) new DetailViewModel(mApp, mWallet);
        }
    }
    private final static int HISTORY_BATCH_COUNT = 10;

    private final Wallet mWallet;
    private Wallets mService = Wallets.getInstance();

    private MutableLiveData<HistoryList> mHistory;
    private MutableLiveData<Transaction.Direction> mDirectionFilter;
    private MutableLiveData<Boolean> mPendingFilter;
    private MutableLiveData<Boolean> mSuccessFilter;
    private MutableLiveData<Pair<Long, Long>> mTime;

    public DetailViewModel(@NonNull Application application, Wallet wallet) {
        super(application);
        mWallet = wallet;
        mHistory = new MutableLiveData<>();
        mHistory.setValue(new HistoryList());

        mDirectionFilter = new MutableLiveData<>();
        mPendingFilter = new MutableLiveData<>();
        mSuccessFilter = new MutableLiveData<>();
        mTime = new MutableLiveData<>();
    }

    public LiveData<HistoryList> getHistory() {
        if (!mHistory.getValue().init) {
            fetchHistory(0);
        }
        return mHistory;
    }

    public void fetchHistory(int start) {

        // skip loading
        if (mHistory.getValue().loading) {
            return;
        }
        // skip no more
        if (!mHistory.getValue().hasMore) {
            return;
        }
        updateHistory(true, null, true);

        final int count = HISTORY_BATCH_COUNT;
        final Map<String, Object> filter = new HashMap<>();
        if (mDirectionFilter.getValue() != null) {
            filter.put("direction", mDirectionFilter.getValue());
        }
        if (mPendingFilter.getValue() != null) {
            filter.put("pending", mPendingFilter.getValue());
        }
        if (mSuccessFilter.getValue() != null) {
            filter.put("success", mSuccessFilter.getValue());
        }
        if (mTime.getValue() != null) {
            filter.put("start_time", mTime.getValue().first);
            filter.put("end_time", mTime.getValue().second);
        }

        mService.getHistory(mWallet.currency, mWallet.tokenAddress, mWallet.address, start, count, filter,
                new Callback<GetHistoryResult>() {
                    @Override
                    public void onError(Throwable error) {
                        Helpers.showToast(getApplication(), "getHistory failed: " + error.getMessage());
                    }

                    @Override
                    public void onResult(GetHistoryResult result) {
                        updateHistory(false, result, result.start + result.transactions.length < result.total);
                    }
                });
    }

    public void refreshHistory() {
        mHistory.setValue(new HistoryList());
        fetchHistory(0);
    }

    private void updateHistory(boolean loading, GetHistoryResult result, boolean hasMore) {
        HistoryList newHistory = new HistoryList();
        newHistory.loading = loading;

        final HistoryList oldHistory = mHistory.getValue();
        if (oldHistory != null) {
            newHistory.history.addAll(oldHistory.history);
            newHistory.init = oldHistory.init;
        }
        if (result != null) {
            newHistory.history.addAll(Arrays.asList(result.transactions));
            newHistory.hasMore = hasMore;
            newHistory.init = true;
        }

        mHistory.setValue(newHistory);
    }

    public void setDirectionFilter(Transaction.Direction direction) {
        mDirectionFilter.setValue(direction);
        refreshHistory();
    }

    public void setPendingFilter(Boolean pending) {
        mPendingFilter.setValue(pending);
        refreshHistory();
    }

    public void setSuccessFilter(Boolean success) {
        mSuccessFilter.setValue(success);
        refreshHistory();
    }

    public void setTime(Pair<Long, Long> time) {
        mTime.setValue(time);
        refreshHistory();
    }
}

/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail.eos;

import android.app.Application;
import android.text.TextUtils;

import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.EosResourceTransactionType;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.GetEosRamPriceResult;
import com.cybavo.wallet.service.wallet.results.GetEosResourcesStateResult;

import java.math.BigDecimal;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import static com.cybavo.wallet.service.wallet.EosResourceTransactionType.*;

public class EOSResourcesViewModel extends AndroidViewModel {

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
            return (T) new EOSResourcesViewModel(mApp, mWallet);
        }
    }
    private final Wallet mWallet;
    private final Wallets mService = Wallets.getInstance();
    private final MutableLiveData<GetEosResourcesStateResult> mResourcesState;
    private final MutableLiveData<EosResourceTransactionType> mTransactionType;
    private final MutableLiveData<GetEosRamPriceResult> mRamPrice;
    private final MutableLiveData<String> mAmount;
    private final MutableLiveData<Long> mNumBytes;
    private final MutableLiveData<String> mReceiver;
    private final MediatorLiveData<Boolean> mValid;
    private final LiveData<BigDecimal> mCpuStaked;
    private final LiveData<BigDecimal> mCpuRefunding;
    private final LiveData<BigDecimal> mNetStaked;
    private final LiveData<BigDecimal> mNetRefunding;
    private final MutableLiveData<Boolean> mInProgress;


    public EOSResourcesViewModel(@NonNull Application application, Wallet wallet) {
        super(application);
        mWallet = wallet;
        mResourcesState = new MutableLiveData<>();
        mResourcesState.setValue(null);

        mRamPrice = new MutableLiveData<>();
        mRamPrice.setValue(null);

        mTransactionType = new MutableLiveData<>();
        mTransactionType.setValue(BUY_RAM);

        mAmount = new MutableLiveData<>();
        mAmount.setValue("0");

        mNumBytes = new MutableLiveData<>();
        mNumBytes.setValue(0L);

        mReceiver = new MutableLiveData<>();
        mReceiver.setValue("");


        mValid = new MediatorLiveData<>();
        mValid.addSource(mTransactionType, type ->
                mValid.setValue(validate(type, mAmount.getValue(), mNumBytes.getValue(), mReceiver.getValue())));
        mValid.addSource(mAmount, amount ->
                mValid.setValue(validate(mTransactionType.getValue(), amount, mNumBytes.getValue(), mReceiver.getValue())));
        mValid.addSource(mNumBytes, numBytes ->
                mValid.setValue(validate(mTransactionType.getValue(), mAmount.getValue(), numBytes, mReceiver.getValue())));
        mValid.addSource(mReceiver, receiver ->
                    mValid.setValue(validate(mTransactionType.getValue(), mAmount.getValue(), mNumBytes.getValue(), receiver)));

        mCpuStaked = Transformations.map(mResourcesState, state ->
                (state != null) ? calcWithPrec(state.cpuAmount, state.cpuAmountPrecision) : null);
        mCpuRefunding = Transformations.map(mResourcesState, state ->
                (state != null) ? calcWithPrec(state.cpuRefund, state.cpuRefundPrecision) : null);
        mNetStaked = Transformations.map(mResourcesState, state ->
                (state != null) ? calcWithPrec(state.netAmount, state.netAmountPrecision) : null);
        mNetRefunding = Transformations.map(mResourcesState, state ->
                (state != null) ? calcWithPrec(state.netRefund, state.netRefundPrecision) : null);

        mInProgress = new MutableLiveData<>();
        mInProgress.setValue(false);
    }

    public LiveData<GetEosResourcesStateResult> getResourcesState() {
        if (mResourcesState.getValue() == null) {
            fetchResourcesState();
        }
        return mResourcesState;
    }

    public void fetchResourcesState() {

        if (mResourcesState.getValue() != null) {
            return;
        }

        mService.getEosResourceState(mWallet.address, new Callback<GetEosResourcesStateResult>() {
                    @Override
                    public void onError(Throwable error) {
                        Helpers.showToast(getApplication(), "getEOSResourceState failed: " + error.getMessage());
                    }

                    @Override
                    public void onResult(GetEosResourcesStateResult result) {
                        mResourcesState.setValue(result);
                    }
                });
    }

    public LiveData<GetEosRamPriceResult> getRamPrice() {
        if (mRamPrice.getValue() == null) {
            fetchEosRamPrice();
        }
        return mRamPrice;
    }

    public void fetchEosRamPrice() {

        if (mRamPrice.getValue() != null) {
            return;
        }

        setInProgress(true);
        mService.getEosRamPrice(new Callback<GetEosRamPriceResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getApplication(), "GetEosRamPriceResult failed: " + error.getMessage());
                setInProgress(false);
            }

            @Override
            public void onResult(GetEosRamPriceResult result) {
                mRamPrice.setValue(result);
                setInProgress(false);
            }
        });
    }

    public void refresh() {
        mResourcesState.setValue(null);
        fetchResourcesState();

        mRamPrice.setValue(null);
        fetchEosRamPrice();
    }

    public LiveData<EosResourceTransactionType> getTransactionType() {
        return mTransactionType;
    }

    public void setTransactionType(EosResourceTransactionType transactionType) {
        mTransactionType.setValue(transactionType);
    }

    public LiveData<BigDecimal> getCpuStaked() {
        return mCpuStaked;
    }

    public LiveData<BigDecimal> getCpuRefunding() {
        return mCpuRefunding;
    }

    public LiveData<BigDecimal> getNetStaked() {
        return mNetStaked;
    }

    public LiveData<BigDecimal> getNetRefunding() {
        return mNetRefunding;
    }

    public void setAmount(String amount) {
        mAmount.setValue(amount);
    }

    public LiveData<String> getAmount() {
        return mAmount;
    }

    public void setNumBytes(long numBytes) {
        mNumBytes.setValue(numBytes);

        GetEosRamPriceResult result = getRamPrice().getValue();
        if (result != null) {
            BigDecimal price = new BigDecimal(result.ramPrice);
            BigDecimal amount = price.multiply(BigDecimal.valueOf((double) numBytes / 1024));
            setAmount(amount.toPlainString());
        }
    }

    public LiveData<Long> getNumBytes() {
        return mNumBytes;
    }


    public void setReceiver(String receiver) {
        mReceiver.setValue(receiver);
    }

    public LiveData<String> getReceiver() {
        return mReceiver;
    }

    public LiveData<Boolean> getValid() {
        return mValid;
    }

    public LiveData<Boolean> getInProgress() {
        return mInProgress;
    }

    public void setInProgress(boolean inProgress) {
        mInProgress.setValue(inProgress);
    }

    private BigDecimal calcWithPrec(long amount, int precision) {
        final BigDecimal div = BigDecimal.valueOf(10).pow(precision);
        final BigDecimal big = BigDecimal.valueOf(amount);
        return big.divide(div);
    }

    private boolean validate(EosResourceTransactionType transactionType, String amount, long numBytes, String receiver) {
        switch (transactionType) {
            case BUY_RAM:
                return numBytes > 0 && !TextUtils.isEmpty(receiver);
            case SELL_RAM:
                return numBytes > 0;
            case DELEGATE_CPU:
            case DELEGATE_NET:
                return toLongSafely(amount) > 0 && !TextUtils.isEmpty(receiver);
            case UNDELEGATE_CPU:
            case UNDELEGATE_NET:
                return toLongSafely(amount) > 0;
            default:
                return false;
        }
    }

    private long toLongSafely(String amount) {
        try {
            return Long.parseLong(amount);
        } catch (Exception e) {
            //
            return 0L;
        }
    }
}

/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.pincode;

import android.app.Application;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class SetupViewModel extends AndroidViewModel {

    public static class Factory implements ViewModelProvider.Factory {

        private final Application mApp;

        public Factory(Application application) {
            mApp = application;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new SetupViewModel(mApp);
        }
    }

    final private MutableLiveData<String> mVerifyCode;
    final private MutableLiveData<String> mPinCode;
    final private LiveData<Boolean> mPinCodeValid;

    final int QUESTIONS_COUNT = 3;
    final private String[] mAllQuestions;
    final private List<LiveData<List<String>>> mAvailableQuestions = new ArrayList<>();

    final private List<MutableLiveData<String>> mQuestions = new ArrayList<>();
    final private List<MutableLiveData<String>> mAnswers = new ArrayList<>();

    public SetupViewModel(@NonNull Application application) {
        super(application);

        mVerifyCode = new MutableLiveData<>();
        mVerifyCode.setValue("");

        mPinCode = new MutableLiveData<>();
        mPinCode.setValue("");

        mPinCodeValid = Transformations.map(mPinCode, Helpers::isPinCodeValid);

        for (int i = 0; i < QUESTIONS_COUNT; i++) {
            MutableLiveData<String> data = new MutableLiveData<>();
            data.setValue("");
            mQuestions.add(data);
        }

        for (int i = 0; i < QUESTIONS_COUNT; i++) {
            MutableLiveData<String> data = new MutableLiveData<>();
            data.setValue("");
            mAnswers.add(data);
        }

        mAllQuestions = application.getResources().getStringArray(R.array.backup_questions);
        for (int i = 0; i < QUESTIONS_COUNT; i++) {
            mAvailableQuestions.add(makeAvailableQuestions(i));
        }
    }

    private LiveData<List<String>> makeAvailableQuestions(int index) {
        MediatorLiveData<List<String>> result = new MediatorLiveData<>();
        List<LiveData<String>> sources = new ArrayList<>();
        for (int i = 0; i < QUESTIONS_COUNT; i++) {
            if (index == i) continue; // skip self
            sources.add(mQuestions.get(i));
        }

        for (LiveData<String> source : sources) {
            result.addSource(source, x -> { // mutual exclude with others
                List<String> available = new ArrayList<>();
                List<String> selected = new ArrayList<>();
                for (LiveData<String> src : sources) {
                    selected.add(src.getValue());
                }
                for (String q : mAllQuestions) {
                    if (selected.contains(q)) continue; // skip selected
                    available.add(q);
                }
                result.setValue(available);
            });
        }

        return result;
    }

    public LiveData<String> getPinCode() {
        return mPinCode;
    }

    public LiveData<Boolean> getPinCodeValid() {
        return mPinCodeValid;
    }

    public void setPinCode(String pinCode) {
        mPinCode.setValue(pinCode);
    }

    public LiveData<List<String>> getAvailableQuestions(int index) {
        return mAvailableQuestions.get(index);
    }

    public void setQuestion(int index, String question) {
        mQuestions.get(index).setValue(question);
    }

    public LiveData<String> getQuestion(int index) {
        return mQuestions.get(index);
    }

    public void setAnswer(int index, String answer) {
        mAnswers.get(index).setValue(answer);
    }

    public LiveData<String> getAnswer(int index) {
        return mAnswers.get(index);
    }

    public LiveData<String> getVerifyCode() {
        return mVerifyCode;
    }

    public void setRecoveryCode(String verifyCode) {
        mVerifyCode.setValue(verifyCode);
    }
}

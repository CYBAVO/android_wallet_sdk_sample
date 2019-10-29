/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.pincode;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cybavo.example.wallet.NavFragment;
import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.PinSecret;
import com.cybavo.wallet.service.auth.results.ChangePinCodeResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChangePinFragment extends Fragment implements InputPinCodeDialog.OnPinCodeInputListener {

    private static final String TAG = ChangePinFragment.class.getSimpleName();

    public ChangePinFragment() {
        // Required empty public constructor
    }

    public static ChangePinFragment newInstance() {
        ChangePinFragment fragment = new ChangePinFragment();
        return fragment;
    }

    private Auth mAuth;

    private TextView mCurrentPinCode;
    private TextView mNewPinCode;
    private Button mSubmit;

    private boolean mCurrentOrNew;
    private PinSecret mCurrentPinSecret = null;
    private PinSecret mNewPinSecret = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_change_pin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_change_pin)
                .onBack(v -> quit())
                .done();

        mAuth = Auth.getInstance();

        mCurrentPinCode = view.findViewById(R.id.currentPinCode);
        mCurrentPinCode.setOnClickListener(v -> inputPinCode(true));

        mNewPinCode = view.findViewById(R.id.newPinCode);
        mNewPinCode.setOnClickListener(v -> inputPinCode(false));

        mSubmit = view.findViewById(R.id.submit);
        mSubmit.setOnClickListener(v -> {
            changePinCode(mCurrentPinSecret, mNewPinSecret);
        });
        mSubmit.setEnabled(false);
    }

    private void inputPinCode(boolean currentOrNew) {
        mCurrentOrNew = currentOrNew;
        InputPinCodeDialog dialog = InputPinCodeDialog.newInstance();
        dialog.show(getChildFragmentManager(), "pinCode");
    }

    private void quit() {
        getFragmentManager().popBackStack();
    }

    private void setInProgress(boolean inProgress) {
        mCurrentPinCode.setEnabled(!inProgress);
        mNewPinCode.setEnabled(!inProgress);
        mSubmit.setEnabled(!inProgress);
    }

    private void changePinCode(PinSecret currentPinSecret, PinSecret newPinSecret) {
        if (currentPinSecret == null || newPinSecret == null) {
            return;
        }

        setInProgress(true);
        mAuth.changePinCode(newPinSecret, currentPinSecret, new Callback<ChangePinCodeResult>() {
            @Override
            public void onError(Throwable error) {
                Log.w(TAG, "changePinCode failed", error);
                setInProgress(false);
                Helpers.showToast(getContext(), "changePinCode failed: " + error.getMessage());
            }

            @Override
            public void onResult(ChangePinCodeResult result) {
                setInProgress(false);
                Helpers.showToast(getContext(), getString(R.string.message_change_pin_success));
                quit();
            }
        });
    }

    @Override
    public void onPinCodeInput(PinSecret pinSecret) {
        if (mCurrentOrNew) {
            mCurrentPinSecret = pinSecret;
            mCurrentPinCode.setText("******");
        } else {
            mNewPinSecret = pinSecret;
            mNewPinCode.setText("******");
        }
        mSubmit.setEnabled(mCurrentPinSecret != null && mNewPinSecret != null);
    }

    @Override
    public void onForgotPinCode() {
        NavFragment.find(this).goRestore();
    }
}

/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.pincode;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.auth.PinSecret;
import com.cybavo.wallet.service.view.NumericPinCodeInputView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class SetupPinFragment extends Fragment {

    public SetupPinFragment() {
        // Required empty public constructor
    }

    public static SetupPinFragment newInstance() {
        SetupPinFragment fragment = new SetupPinFragment();
        return fragment;
    }

    private SetupViewModel mSetupViewModel;
    private NumericPinCodeInputView mPinCodeInput;
    private TextView mPinCode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_pin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPinCode = view.findViewById(R.id.pinCode);
        mPinCodeInput = view.findViewById(R.id.pinCodeInput);

        final int pinLength = mPinCodeInput.getMaxLength();
        mPinCode.setText(Helpers.makePlaceholder(0, pinLength));
        mPinCodeInput.setOnPinCodeInputListener(length -> {
            mPinCode.setText(Helpers.makePlaceholder(length, pinLength));

            // pin code length fulfilled
            if (length >= pinLength) {
                final PinSecret pinSecret = mPinCodeInput.submit();
                mSetupViewModel.setPinSecret(pinSecret);
            } else {
                mSetupViewModel.setPinSecret(null);
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSetupViewModel = ViewModelProviders.of(getParentFragment(),
                new SetupViewModel.Factory(getActivity().getApplication())).get(SetupViewModel.class);
    }
}

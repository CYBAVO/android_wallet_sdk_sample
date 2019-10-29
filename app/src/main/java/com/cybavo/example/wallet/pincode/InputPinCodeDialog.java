/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.pincode;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.auth.PinSecret;
import com.cybavo.wallet.service.view.PinCodeInputView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class InputPinCodeDialog extends DialogFragment {

    public interface OnPinCodeInputListener {
        void onPinCodeInput(PinSecret pinSecret);
        void onForgotPinCode();
    }

    private static final String TAG = InputPinCodeDialog.class.getSimpleName();
    private static final String ARG_SETUP = "setup";

    public static InputPinCodeDialog newInstance() {
        return newInstance(false);
    }

    public static InputPinCodeDialog newInstance(boolean setup) {
        InputPinCodeDialog dialog = new InputPinCodeDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SETUP, setup);
        dialog.setArguments(args);
        return dialog;
    }

    public InputPinCodeDialog() {
    }

    private boolean mSetup;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSetup = getArguments().getBoolean(ARG_SETUP);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View view = inflater.inflate(R.layout.dialog_input_pin_code, null, false);
        final TextView pinCode = view.findViewById(R.id.pinCode);
        final PinCodeInputView pinCodeInput = view.findViewById(R.id.pinCodeInput);
        final int pinLength = pinCodeInput.getMaxLength();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(mSetup ? R.string.title_input_new_pin : R.string.title_input_pin)
                .setNegativeButton(android.R.string.cancel, null);

        if (!mSetup) {
            builder.setNeutralButton(R.string.action_forgot_pin, (dlg, which) -> {
                onForgotPinCode();
            });
        }
        AlertDialog dialog = builder.create();


        pinCode.setText(Helpers.makePlaceholder(0, pinLength));
        pinCodeInput.setOnPinCodeInputListener(length -> {
            pinCode.setText(Helpers.makePlaceholder(length, pinLength));

            // pin code length fulfilled
            if (length >= pinLength) {
                final PinSecret pinSecret = pinCodeInput.submit();
                onInputPinCode(pinSecret);
                dialog.dismiss();
            }
        });

        return dialog;
    }

    private void onInputPinCode(PinSecret pinCode) {
        if (getParentFragment() instanceof OnPinCodeInputListener) {
            ((OnPinCodeInputListener) getParentFragment()).onPinCodeInput(pinCode);
        }
    }

    private void onForgotPinCode() {
        if (getParentFragment() instanceof OnPinCodeInputListener) {
            ((OnPinCodeInputListener) getParentFragment()).onForgotPinCode();
        }
    }
}

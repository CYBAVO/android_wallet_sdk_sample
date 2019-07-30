/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.pincode;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class InputPinCodeDialog extends DialogFragment {

    public interface OnPinCodeInputListener {
        void onPinCodeInput(String pinCode);
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
        final EditText pinCodeEdit = view.findViewById(R.id.pinCode);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(mSetup ? R.string.title_input_new_pin : R.string.title_input_pin)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null);

        if (!mSetup) {
            builder.setNeutralButton(R.string.action_forgot_pin, (dlg, which) -> {
                onForgotPinCode();
            });
        }
        AlertDialog dialog = builder.create();

        // to prevent dismiss when button clicked
        dialog.setOnShowListener(dlg -> {
            Button button = ((AlertDialog) dlg).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                if (onInputPinCode(pinCodeEdit.getText().toString())) {
                    dlg.dismiss();
                }
            });
            button.setEnabled(false);
        });

        pinCodeEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setEnabled(s.length() >= getResources().getInteger(R.integer.pin_code_length));
            }
        });

        return dialog;
    }

    private boolean onInputPinCode(String pinCode) {
        if (getParentFragment() instanceof OnPinCodeInputListener) {
            ((OnPinCodeInputListener) getParentFragment()).onPinCodeInput(pinCode);
        }
        return true;
    }

    private void onForgotPinCode() {
        if (getParentFragment() instanceof OnPinCodeInputListener) {
            ((OnPinCodeInputListener) getParentFragment()).onForgotPinCode();
        }
    }
}

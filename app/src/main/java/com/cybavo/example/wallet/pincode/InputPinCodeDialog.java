package com.cybavo.example.wallet.pincode;

import android.app.Dialog;
import android.os.Bundle;
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
        final EditText pinCodeEdit = view.findViewById(R.id.newPinCode);

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
        });
        return dialog;
    }

    private boolean onInputPinCode(String pinCode) {
        if (!Helpers.isPinCodeValid(pinCode)) {
            Helpers.showToast(getContext(), getString(R.string.message_invalid_pin));
            return false;
        }
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

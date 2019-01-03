package com.cybavo.example.wallet.pincode;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.api.Error;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.results.ChangePinCodeResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChangePinFragment extends Fragment {

    private static final String TAG = ChangePinFragment.class.getSimpleName();

    public ChangePinFragment() {
        // Required empty public constructor
    }

    public static ChangePinFragment newInstance() {
        ChangePinFragment fragment = new ChangePinFragment();
        return fragment;
    }

    private Auth mAuth;

    private EditText mCurrentPinCodeEdit;
    private EditText mNewPinCodeEdit;
    private Button mSubmit;

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

        mCurrentPinCodeEdit = view.findViewById(R.id.currentPinCode);
        mNewPinCodeEdit = view.findViewById(R.id.newPinCode);

        mSubmit = view.findViewById(R.id.submit);
        mSubmit.setOnClickListener(v -> {
            changePinCode(mCurrentPinCodeEdit.getText().toString(),
                    mNewPinCodeEdit.getText().toString());
        });
    }

    private void quit() {
        getFragmentManager().popBackStack();
    }

    private void setInProgress(boolean inProgress) {
        mCurrentPinCodeEdit.setEnabled(!inProgress);
        mNewPinCodeEdit.setEnabled(!inProgress);
        mSubmit.setEnabled(!inProgress);
    }

    private void changePinCode(String currentPinCode, String newPinCode) {
        if (currentPinCode.isEmpty() || newPinCode.isEmpty()) {
            return;
        }

        if (!Helpers.isPinCodeValid(newPinCode)) {
            Helpers.showToast(getContext(), getString(R.string.message_invalid_pin));
            return;
        }

        setInProgress(true);
        mAuth.changePinCode(newPinCode, currentPinCode, new Callback<ChangePinCodeResult>() {
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
}

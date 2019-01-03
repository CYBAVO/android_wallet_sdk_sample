package com.cybavo.example.wallet.pincode;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.results.ForgotPinCodeResult;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class VerifyCodeFragment extends Fragment {

    private static final String TAG = VerifyCodeFragment.class.getSimpleName();

    public VerifyCodeFragment() {
        // Required empty public constructor
    }

    public static VerifyCodeFragment newInstance() {
        VerifyCodeFragment fragment = new VerifyCodeFragment();
        return fragment;
    }

    private SetupViewModel mSetupViewModel;
    private Button mForgot;
    private EditText mVerifyCodeEdit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_verify_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mForgot = view.findViewById(R.id.action_forgot);
        mForgot.setOnClickListener(v -> forgotPinCode());

        mVerifyCodeEdit = view.findViewById(R.id.verifyCode);
        mVerifyCodeEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mSetupViewModel.setVerifyCode(s.toString());
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSetupViewModel = ViewModelProviders.of(getParentFragment(),
                new SetupViewModel.Factory(getActivity().getApplication())).get(SetupViewModel.class);
    }

    private void forgotPinCode() {
        mForgot.setEnabled(false);
        Auth.getInstance().forgotPinCode(new Callback<ForgotPinCodeResult>() {
            @Override
            public void onError(Throwable error) {
                Log.w(TAG, "forgotPinCode failed", error);
                Helpers.showToast(getContext(), "forgotPinCode failed: " + error.getMessage());
                mForgot.setEnabled(true);
            }

            @Override
            public void onResult(ForgotPinCodeResult forgotPinCodeResult) {
                mForgot.setEnabled(false);
                mForgot.setText(String.format(Locale.getDefault(), "%s %s", getString(R.string.action_forgot), "✓"));
            }
        });
    }
}

/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.pay;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.create.WalletDropdownAdapter;
import com.cybavo.example.wallet.detail.FeeAdapter;
import com.cybavo.example.wallet.helper.CurrencyHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.example.wallet.pincode.InputPinCodeDialog;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.auth.PinSecret;
import com.cybavo.wallet.service.wallet.Fee;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.CreateTransactionResult;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class PayFragment extends Fragment implements InputPinCodeDialog.OnPinCodeInputListener {

    public PayFragment() {
        // Required empty public constructor
    }

    public static PayFragment newInstance(long currency, String tokenAddress, String amount, String targetAddress) {
        PayFragment fragment = new PayFragment();
        Bundle args = new Bundle();
        args.putLong(PayActivity.ARG_PAY_CURRENCY, currency);
        args.putString(PayActivity.ARG_PAY_TOKEN_ADDRESS, tokenAddress);
        args.putString(PayActivity.ARG_PAY_AMOUNT, amount);
        args.putString(PayActivity.ARG_PAY_TARGET_ADDRESS, targetAddress);
        fragment.setArguments(args);
        return fragment;
    }

    private Wallets mService;
    private long mPayCurrency;
    private String mPayTokenAddress;
    private String mPayAmount;
    private String mPayTargetAddress;

    private TextView mBalance;
    private TextView mQuota;
    private TextView mUsage;
    private EditText mAmount;
    private EditText mAddress;

    private PayViewModel mViewModel;

    private Spinner mFeeSpinner;
    private FeeAdapter mFeeAdapter;

    private Spinner mWalletSpinner;
    private WalletDropdownAdapter mWalletAdapter;

    private Button mSubmit;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPayCurrency = getArguments().getLong(PayActivity.ARG_PAY_CURRENCY);
            mPayTokenAddress = getArguments().getString(PayActivity.ARG_PAY_TOKEN_ADDRESS);
            mPayAmount = getArguments().getString(PayActivity.ARG_PAY_AMOUNT);
            mPayTargetAddress = getArguments().getString(PayActivity.ARG_PAY_TARGET_ADDRESS);
        }
        mService = Wallets.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pay, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_pay)
                .onBack(v -> quit(Activity.RESULT_CANCELED))
                .done();

        mBalance = view.findViewById(R.id.balance);
        mQuota = view.findViewById(R.id.quota);
        mUsage = view.findViewById(R.id.usage);

        mWalletSpinner = view.findViewById(R.id.wallet);
        mWalletAdapter = new WalletDropdownAdapter(getContext());
        mWalletSpinner.setAdapter(mWalletAdapter);
        mWalletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Wallet wallet = (Wallet) parent.getAdapter().getItem(position);
                mViewModel.setSelectedWallet(wallet);
            }
        });

        mAmount = view.findViewById(R.id.amount);
        mAmount.setText(mPayAmount);
        mAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mPayAmount = s.toString();
            }
        });

        mAddress = view.findViewById(R.id.address);
        mAddress.setText(mPayTargetAddress);

        mFeeSpinner = view.findViewById(R.id.fee);
        mFeeAdapter = new FeeAdapter(getContext());
        mFeeSpinner.setAdapter(mFeeAdapter);

        mSubmit = view.findViewById(R.id.submit);
        mSubmit.setOnClickListener(v -> {
            inputPinCode();
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = ViewModelProviders.of(getActivity(),
                new PayViewModel.Factory(getActivity().getApplication(), mPayCurrency, mPayTokenAddress))
                .get(PayViewModel.class);

        mViewModel.getAvailableWallets().observe(this, wallets -> {
            mWalletAdapter.clear();
            mWalletAdapter.addAll(wallets);
        });

        mViewModel.getBalance().observe(this, entry -> {
            final String symbol = mViewModel.getSelectedWallet().getValue() != null ? mViewModel.getSelectedWallet().getValue().currencySymbol : "";
            if (entry.init) {
                mBalance.setText(getString(R.string.template_amount, CurrencyHelper.getEffectiveBalance(entry.balance), symbol));
            } else {
                mBalance.setText(getString(R.string.template_amount, "…", symbol));
            }
        });

        mViewModel.getUsage().observe(this, usage -> {
            final String symbol = mViewModel.getSelectedWallet().getValue() != null ? mViewModel.getSelectedWallet().getValue().currencySymbol : "";
            if (usage == null) { // fetching
                mQuota.setText(getString(R.string.template_quota, "…", symbol));
                mUsage.setText(getString(R.string.template_usage, "…", symbol));
            } else {
                mQuota.setText(getString(R.string.template_quota, usage.dailyTransactionAmountQuota, symbol));
                mUsage.setText(getString(R.string.template_usage, usage.dailyTransactionAmountUsage, symbol));
            }
        });

        mViewModel.getTransactionFee().observe(this, fees -> {
            mFeeAdapter.clear();
            mFeeAdapter.addAll(fees);
        });
    }

    private void inputPinCode() {
        InputPinCodeDialog dialog = InputPinCodeDialog.newInstance();
        dialog.show(getChildFragmentManager(), "pinCode");
    }

    @Override
    public void onPinCodeInput(PinSecret pinSecret) {
        final String toAddress = mPayTargetAddress;
        final String amount = mPayAmount;
        final Fee fee = (Fee) mFeeSpinner.getSelectedItem();
        createTransaction(toAddress, amount, fee, pinSecret);
    }

    @Override
    public void onForgotPinCode() {
        // ignore
    }

    private void setInProgress(boolean inProgress) {
        mAddress.setEnabled(!inProgress);
        mAmount.setEnabled(!inProgress);
        mFeeSpinner.setEnabled(!inProgress);
        mWalletSpinner.setEnabled(!inProgress);
        mSubmit.setEnabled(!inProgress);
    }

    private void createTransaction(String toAddress, String amount, Fee fee, PinSecret pinSecret) {
        if (toAddress.isEmpty() || amount.isEmpty() || fee == null || pinSecret == null) {
            return;
        }

        final Wallet wallet = mViewModel.getSelectedWallet().getValue();
        if (wallet == null) {
            return;
        }

        setInProgress(true);
        mService.createTransaction(wallet.walletId, toAddress, amount, fee.amount, "", pinSecret, new HashMap<>(), new Callback<CreateTransactionResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getContext(), "createTransaction failed: " + error.getMessage());
                setInProgress(false);
            }

            @Override
            public void onResult(CreateTransactionResult result) {
                setInProgress(false);
                quit(Activity.RESULT_OK);
            }
        });
    }

    private void quit(int result) {
        if (getActivity() != null) {
            getActivity().setResult(result);
            getActivity().finish();
        }
    }
}
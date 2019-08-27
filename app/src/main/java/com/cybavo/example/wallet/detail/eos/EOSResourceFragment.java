/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail.eos;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.cybavo.example.wallet.NavFragment;
import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.detail.WalletParcelable;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.example.wallet.main.MainViewModel;
import com.cybavo.example.wallet.pincode.InputPinCodeDialog;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.EosResourceTransactionType;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.CreateTransactionResult;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import static com.cybavo.wallet.service.wallet.EosResourceTransactionType.BUY_RAM;
import static com.cybavo.wallet.service.wallet.EosResourceTransactionType.DELEGATE_CPU;
import static com.cybavo.wallet.service.wallet.EosResourceTransactionType.DELEGATE_NET;
import static com.cybavo.wallet.service.wallet.EosResourceTransactionType.SELL_RAM;
import static com.cybavo.wallet.service.wallet.EosResourceTransactionType.UNDELEGATE_CPU;
import static com.cybavo.wallet.service.wallet.EosResourceTransactionType.UNDELEGATE_NET;

public class EOSResourceFragment extends Fragment implements InputPinCodeDialog.OnPinCodeInputListener {

    private static final String TAG = EOSResourceFragment.class.getSimpleName();
    private static final String ARG_WALLET = "wallet";

    private Wallet mWallet;

    private MainViewModel mViewModel;
    private EOSResourcesViewModel mResourcesViewModal;

    private static final SparseArray<EosResourceTransactionType> TRANSACTION_TYPES = new SparseArray();
    static {
        TRANSACTION_TYPES.put(R.id.buyRam, BUY_RAM);
        TRANSACTION_TYPES.put(R.id.sellRam, SELL_RAM);
        TRANSACTION_TYPES.put(R.id.delegateCpu, DELEGATE_CPU);
        TRANSACTION_TYPES.put(R.id.undelegateCpu, UNDELEGATE_CPU);
        TRANSACTION_TYPES.put(R.id.delegateNet, DELEGATE_NET);
        TRANSACTION_TYPES.put(R.id.undelegateNet, UNDELEGATE_NET);
    }

    private ProgressBar mProgressRam;
    private ProgressBar mProgressCpu;
    private ProgressBar mProgressNET;
    private TextView mDescRam;
    private TextView mDescCpu;
    private TextView mDescNet;
    private TextView mRamPrice;
    private TextView mStakeCpu;
    private TextView mStakeNet;

    private RadioGroup mTransactionType;
    private TextInputLayout mAmount;
    private TextInputLayout mNumBytes;
    private TextInputLayout mReceiver;

    private Button mSubmit;

    public static EOSResourceFragment newInstance(Wallet wallet) {
        EOSResourceFragment fragment = new EOSResourceFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLET, WalletParcelable.fromWallet(wallet));
        fragment.setArguments(args);
        return fragment;
    }

    public EOSResourceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWallet = WalletParcelable.toWallet(getArguments().getParcelable(ARG_WALLET));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_eos_resource, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_eos_resources)
                .onBack(v -> getFragmentManager().popBackStack())
                .done();

        mProgressCpu = view.findViewById(R.id.progressCpu);
        mProgressRam = view.findViewById(R.id.progressRam);
        mProgressNET = view.findViewById(R.id.progressNet);
        mDescCpu = view.findViewById(R.id.descCpu);
        mDescRam = view.findViewById(R.id.descRam);
        mDescNet = view.findViewById(R.id.descNet);
        mRamPrice = view.findViewById(R.id.ramPrice);
        mStakeCpu = view.findViewById(R.id.stakeCpu);
        mStakeNet = view.findViewById(R.id.stakeNet);

        mTransactionType = view.findViewById(R.id.transactionType);

        mAmount = view.findViewById(R.id.amountWrapper);
        mNumBytes = view.findViewById(R.id.numBytesWrapper);
        mReceiver = view.findViewById(R.id.receiverWrapper);

        mSubmit = view.findViewById(R.id.submit);
        mSubmit.setOnClickListener(v -> {
            inputPinCode();
        });
    }

    private final static int PROGRESS_MAX = 100;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // detail ViewModel
        mViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);

        mResourcesViewModal = ViewModelProviders.of(this,
                new EOSResourcesViewModel.Factory(getActivity().getApplication(), mWallet))
                .get(EOSResourcesViewModel.class);

        mResourcesViewModal.getResourcesState().observe(this, state -> {
            if (state != null) {
                final float progressCpu = (float) state.cpuUsed / state.cpuMax;
                mProgressCpu.setIndeterminate(false);
                mProgressCpu.setMax(PROGRESS_MAX);
                mProgressCpu.setProgress((int) (progressCpu * PROGRESS_MAX));
                mDescCpu.setText(String.format(Locale.getDefault(), "%d / %d μs", state.cpuUsed, state.cpuMax));

                final float progressRam = (float) state.ramUsage / state.ramQuota;
                mProgressRam.setIndeterminate(false);
                mProgressRam.setMax(PROGRESS_MAX);
                mProgressRam.setProgress((int) (progressRam * PROGRESS_MAX));
                mDescRam.setText(String.format(Locale.getDefault(), "%d / %d bytes", state.ramUsage, state.ramQuota));

                final float progressNET = (float) state.netUsed / state.netMax;
                mProgressNET.setIndeterminate(false);
                mProgressNET.setMax(PROGRESS_MAX);
                mProgressNET.setProgress((int) (progressNET * PROGRESS_MAX));
                mDescNet.setText(String.format(Locale.getDefault(), "%d / %d bytes", state.netUsed, state.netMax));
            } else {
                mProgressCpu.setIndeterminate(true);
                mProgressRam.setIndeterminate(true);
                mProgressNET.setIndeterminate(true);
                mDescCpu.setText("…");
                mDescRam.setText("…");
                mDescNet.setText("…");
            }
        });


        mTransactionType.setOnCheckedChangeListener((group, checkedId) -> {
            mResourcesViewModal.setTransactionType(TRANSACTION_TYPES.get(checkedId));
        });
        mResourcesViewModal.getTransactionType().observe(this, type -> {
            updateTransactionType(type);
        });
        mResourcesViewModal.getRamPrice().observe(this, price -> {
            if (price != null) {
                mRamPrice.setText(String.format("%s EOS/Kbytes", price.ramPrice));
            }
        });
        mResourcesViewModal.getCpuStaked().observe(this, staked ->
                updateCpuStaked(staked, mResourcesViewModal.getCpuRefunding().getValue()));
        mResourcesViewModal.getCpuRefunding().observe(this, refunding ->
                updateCpuStaked(mResourcesViewModal.getCpuStaked().getValue(), refunding));
        mResourcesViewModal.getNetStaked().observe(this, staked ->
                updateNetStaked(staked, mResourcesViewModal.getNetRefunding().getValue()));
        mResourcesViewModal.getNetRefunding().observe(this, refunding ->
                updateNetStaked(mResourcesViewModal.getNetStaked().getValue(), refunding));
        mResourcesViewModal.getAmount().observe(this, amount ->
                mNumBytes.setHelperText(String.format("≈ %sEOS", amount)));

        mAmount.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mResourcesViewModal.setAmount(s.toString());
            }
        });

        mNumBytes.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                long numBytes = 0;
                try {
                    numBytes = Long.parseLong(s.toString());
                } catch (Exception e) {
                    // ignore
                }
                mResourcesViewModal.setNumBytes(numBytes);
            }
        });

        mReceiver.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mResourcesViewModal.setReceiver(s.toString());
            }
        });

        mResourcesViewModal.getInProgress().observe(this, inProgress -> {
            mSubmit.setEnabled(!inProgress);
            mAmount.setEnabled(!inProgress);
            mNumBytes.setEnabled(!inProgress);
            mReceiver.setEnabled(!inProgress);
            for (int i = 0; i < mTransactionType.getChildCount(); i++) {
                mTransactionType.getChildAt(i).setEnabled(!inProgress);
            }
        });
    }

    private void reset() {
        mAmount.getEditText().setText("0", TextView.BufferType.EDITABLE);
        mNumBytes.getEditText().setText("0", TextView.BufferType.EDITABLE);
    }

    private void updateTransactionType(EosResourceTransactionType transactionType) {
        // reset inputs
        reset();

        switch (transactionType) {
            case BUY_RAM:
                mSubmit.setText(R.string.action_eos_buy_ram);
                mAmount.setVisibility(View.GONE);
                mNumBytes.setVisibility(View.VISIBLE);
                mReceiver.setVisibility(View.VISIBLE);
                break;
            case SELL_RAM:
                mSubmit.setText(R.string.action_eos_sell_ram);
                mAmount.setVisibility(View.GONE);
                mNumBytes.setVisibility(View.VISIBLE);
                mReceiver.setVisibility(View.GONE);
                break;
            case DELEGATE_CPU:
                mSubmit.setText(R.string.action_eos_delegate_cpu);
                mAmount.setVisibility(View.VISIBLE);
                mNumBytes.setVisibility(View.GONE);
                mReceiver.setVisibility(View.VISIBLE);
                break;
            case UNDELEGATE_CPU:
                mSubmit.setText(R.string.action_eos_undelegate_cpu);
                mAmount.setVisibility(View.VISIBLE);
                mNumBytes.setVisibility(View.GONE);
                mReceiver.setVisibility(View.GONE);
                break;
            case DELEGATE_NET:
                mSubmit.setText(R.string.action_eos_delegate_net);
                mAmount.setVisibility(View.VISIBLE);
                mNumBytes.setVisibility(View.GONE);
                mReceiver.setVisibility(View.VISIBLE);
                break;
            case UNDELEGATE_NET:
                mSubmit.setText(R.string.action_eos_undelegate_net);
                mAmount.setVisibility(View.VISIBLE);
                mNumBytes.setVisibility(View.GONE);
                mReceiver.setVisibility(View.GONE);
                break;
        }
    }

    private void updateCpuStaked(BigDecimal cpuStaked, BigDecimal cpuRefunding) {
        if (cpuStaked == null || cpuRefunding == null) {
            mStakeCpu.setText("…");
        } else {
            mStakeCpu.setText(getString(R.string.message_staked_and_refund, cpuStaked.toPlainString(), cpuRefunding.toPlainString()));
        }
    }

    private void updateNetStaked(BigDecimal netStaked, BigDecimal netRefunding) {
        if (netStaked == null || netRefunding == null) {
            mStakeNet.setText("…");
        } else {
            mStakeNet.setText(getString(R.string.message_staked_and_refund, netStaked.toPlainString(), netRefunding.toPlainString()));
        }
    }


    private void inputPinCode() {
        InputPinCodeDialog dialog = InputPinCodeDialog.newInstance();
        dialog.show(getChildFragmentManager(), "pinCode");
    }

    @Override
    public void onPinCodeInput(String pinCode) {
        createResourceTransaction(pinCode);
    }

    @Override
    public void onForgotPinCode() {
        NavFragment.find(this).goRestore();
    }

    private void createResourceTransaction(String pinCode) {
        final String receiver = mResourcesViewModal.getReceiver().getValue();
        final String amount = mResourcesViewModal.getAmount().getValue();
        final long numBytes = mResourcesViewModal.getNumBytes().getValue();
        final EosResourceTransactionType transactionType = mResourcesViewModal.getTransactionType().getValue();

        mResourcesViewModal.setInProgress(true);

        final Map<String, Object> extras = new HashMap<>();
        extras.put("eos_transaction_type", transactionType);
        extras.put("num_bytes", numBytes);

        Wallets.getInstance().createTransaction(
                mWallet.walletId,
                receiver,
                amount,
                "", // transactionFee
                "", // description
                pinCode,
                extras,
                new Callback<CreateTransactionResult>() {
                    @Override
                    public void onError(Throwable error) {
                        mResourcesViewModal.setInProgress(false);
                        Helpers.showToast(getContext(), "createEosResourceTransaction failed: " + error.getMessage());
                    }

                    @Override
                    public void onResult(CreateTransactionResult createTransactionResult) {
                        mResourcesViewModal.setInProgress(false);
                        mResourcesViewModal.refresh();
                        mViewModel.getBalance(mWallet, true);
                        reset();
                    }
                });
    }
}

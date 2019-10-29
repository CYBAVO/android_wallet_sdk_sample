/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.EstimateTransactionResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ConfirmTransactionDialog extends DialogFragment {

    public interface OnConfirmListener {
        void onConfirm();
    }

    private static final String TAG = ConfirmTransactionDialog.class.getSimpleName();
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_TX_AMOUNT = "tx_amount";
    private static final String ARG_PLAFTORM_FEE = "platform_fee";
    private static final String ARG_BLOCKCHAIN_FEE = "blockchain_fee";

    private String mAddress;
    private String mTransactionAmount;
    private String mPlatformFee;
    private String mBlockchainFee;

    static ConfirmTransactionDialog newInstance(String address, String transactionAmount, String platformFee, String blockchainFee) {
        ConfirmTransactionDialog dialog = new ConfirmTransactionDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS, address);
        args.putString(ARG_TX_AMOUNT, transactionAmount);
        args.putString(ARG_PLAFTORM_FEE, platformFee);
        args.putString(ARG_BLOCKCHAIN_FEE, blockchainFee);
        dialog.setArguments(args);
        return dialog;
    }

    public ConfirmTransactionDialog() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAddress = getArguments().getString(ARG_ADDRESS);
            mTransactionAmount = getArguments().getString(ARG_TX_AMOUNT);
            mPlatformFee = getArguments().getString(ARG_PLAFTORM_FEE);
            mBlockchainFee = getArguments().getString(ARG_BLOCKCHAIN_FEE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View view = inflater.inflate(R.layout.dialog_confirm_transaction, null, false);

        TextView addressLabel = view.findViewById(R.id.addressLabel);
        TextView address = view.findViewById(R.id.address);

        TextView amountLabel = view.findViewById(R.id.amountLabel);
        TextView amountText = view.findViewById(R.id.amount);

        TextView platformFeeLabel = view.findViewById(R.id.platformFeeLabel);
        TextView platformFeeText = view.findViewById(R.id.platformFee);

        TextView blockchainFeeLabel = view.findViewById(R.id.blockchainFeeLabel);
        TextView blockchainFeeText = view.findViewById(R.id.blockchainFee);

        updateValue(addressLabel, address, mAddress);
        updateValue(amountLabel, amountText, mTransactionAmount);
        updateValue(platformFeeLabel, platformFeeText, mPlatformFee);
        updateValue(blockchainFeeLabel, blockchainFeeText, mBlockchainFee);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.title_confirm_transaction)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> {
                    if (getParentFragment() instanceof OnConfirmListener) {
                        ((OnConfirmListener) getParentFragment()).onConfirm();
                    }
                })
                .create();
    }

    private void updateValue(TextView labelView, TextView valueView, String src) {
        if (TextUtils.isEmpty(src) || "0".equals(src)) {
            labelView.setVisibility(View.GONE);
            valueView.setVisibility(View.GONE);
        } else {
            labelView.setVisibility(View.VISIBLE);
            valueView.setVisibility(View.VISIBLE);
            valueView.setText(src);
        }
    }
}

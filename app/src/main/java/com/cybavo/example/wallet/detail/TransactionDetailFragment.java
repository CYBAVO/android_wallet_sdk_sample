/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.CurrencyHelper;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.example.wallet.main.MainViewModel;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Transaction;
import com.cybavo.wallet.service.wallet.Wallet;

import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class TransactionDetailFragment extends Fragment {

    private static final String ARG_WALLET = "wallet";
    private static final String ARG_TRANSACTION = "transaction";

    private MainViewModel mViewModel;
    protected Wallet mWallet;
    private Transaction mTransaction;

    private TextView mWithdraw;
    private TextView mDeposit;
    private TextView mCurrency;
    private TextView mFrom;
    private TextView mTo;
    private TextView mAmount;
    private TextView mFee;
    private TextView mTxid;
    private TextView mError;
    private TextView mTime;
    private TextView mPending;
    private TextView mFailed;
    private Button mExplorer;

    public TransactionDetailFragment() {
        // Required empty public constructor
    }

    public static TransactionDetailFragment newInstance(Wallet wallet, Transaction transaction) {
        TransactionDetailFragment fragment = new TransactionDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLET, WalletParcelable.fromWallet(wallet));
        args.putParcelable(ARG_TRANSACTION, TransactionParcelable.fromTransaction(transaction));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWallet = WalletParcelable.toWallet(getArguments().getParcelable(ARG_WALLET));
            mTransaction = TransactionParcelable.toTransaction(getArguments().getParcelable(ARG_TRANSACTION));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_transaction_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_transaction_detail)
                .onBack(v -> getFragmentManager().popBackStack())
                .done();

        mCurrency = view.findViewById(R.id.currency);
        mCurrency.setText(mWallet.currencySymbol);
        mCurrency.setCompoundDrawablesWithIntrinsicBounds(
                CurrencyHelper.getCoinIconResource(getContext(), mWallet.currencySymbol), 0, 0, 0);

        mWithdraw = view.findViewById(R.id.withdraw);
        mDeposit = view.findViewById(R.id.deposit);
        if (mTransaction.direction == Transaction.Direction.IN) {
            mDeposit.setVisibility(View.VISIBLE);
            mWithdraw.setVisibility(View.GONE);
        } else {
            mDeposit.setVisibility(View.GONE);
            mWithdraw.setVisibility(View.VISIBLE);
        }

        mFrom = view.findViewById(R.id.from);
        mFrom.setText(mTransaction.fromAddress);
        mTo = view.findViewById(R.id.to);
        mTo.setText(mTransaction.toAddress);
        mAmount = view.findViewById(R.id.amount);
        mAmount.setText(getString(R.string.template_amount, mTransaction.amount, mWallet.currencySymbol));
        mFee = view.findViewById(R.id.fee);
        mFee.setText(mTransaction.transactionFee);
        mTxid = view.findViewById(R.id.txid);
        if (!TextUtils.isEmpty(mTransaction.txid)) {
            mTxid.setVisibility(View.VISIBLE);
            mTxid.setText(mTransaction.txid);
        }
        mError = view.findViewById(R.id.error);
        if (!TextUtils.isEmpty(mTransaction.error)) {
            mError.setVisibility(View.VISIBLE);
            mError.setText(mTransaction.error);
        }

        mTime = view.findViewById(R.id.time);
        mTime.setText(DateFormat.format(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMddHHmmss"),
                new Date(mTransaction.timestamp * 1000)));

        mPending = view.findViewById(R.id.pending);
        mPending.setVisibility(mTransaction.pending ? View.VISIBLE : View.GONE);

        mFailed = view.findViewById(R.id.failed);
        mFailed.setVisibility(mTransaction.pending || mTransaction.success ? View.GONE : View.VISIBLE);

        mExplorer = view.findViewById(R.id.explorer);
        final String uri = CurrencyHelper.getBlockExplorerUri(mWallet.currency, mWallet.tokenAddress, mTransaction.txid);
        if (uri != null) {
            mExplorer.setOnClickListener(v -> {
                browse(uri);
            });
        } else {
            mExplorer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);

        mViewModel.getCurrencies().observe(this, currencies -> {
            final Currency c = CurrencyHelper.findCurrency(currencies, mWallet);
            if (c != null) {
                mCurrency.setText(c.displayName);
            } else {
                mCurrency.setText(mWallet.currencySymbol);
            }
        });
    }

    private void browse(String uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

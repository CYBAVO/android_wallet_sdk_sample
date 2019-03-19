package com.cybavo.example.wallet.detail;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.cybavo.example.wallet.NavFragment;
import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.CurrencyHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.example.wallet.main.MainViewModel;
import com.cybavo.example.wallet.pincode.InputPinCodeDialog;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.api.Error;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Fee;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.CreateTransactionResult;
import com.cybavo.wallet.service.wallet.results.RequestSecureTokenResult;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class WithdrawFragment extends Fragment implements InputPinCodeDialog.OnPinCodeInputListener {

    private static final String ARG_WALLET = "wallet";

    private Wallets mService;
    private Wallet mWallet;
    private MainViewModel mViewModel;
    private WithdrawViewModel mWithdrawViewModel;

    private TextView mCurrency;
    private TextView mBalance;
    private TextView mQuota;
    private TextView mUsage;
    private Spinner mFeeSpinner;
    private View mScanAddress;
    private EditText mAddress;
    private EditText mAmount;
    private Button mSubmit;
    private Button mSubmitWithToken;
    private ProgressBar mLoading;

    private FeeAdapter mFeeAdapter;
    private boolean mPinForToken = false;

    public WithdrawFragment() {
    }

    public static WithdrawFragment newInstance(Wallet wallet) {
        WithdrawFragment fragment = new WithdrawFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLET, WalletParcelable.fromWallet(wallet));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWallet = WalletParcelable.toWallet(getArguments().getParcelable(ARG_WALLET));
        }
        mService = Wallets.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_withdraw, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_withdraw)
                .onBack(v -> getFragmentManager().popBackStack())
                .done();

        mCurrency = view.findViewById(R.id.currency);
        mCurrency.setText(mWallet.currencySymbol);
        mCurrency.setCompoundDrawablesWithIntrinsicBounds(
                CurrencyHelper.getCoinIconResource(getContext(), mWallet.currencySymbol), 0, 0, 0);

        mBalance = view.findViewById(R.id.balance);
        mQuota = view.findViewById(R.id.quota);
        mUsage = view.findViewById(R.id.usage);

        mFeeSpinner = view.findViewById(R.id.fee);
        mFeeAdapter = new FeeAdapter(getContext());
        mFeeSpinner.setAdapter(mFeeAdapter);

        mScanAddress = view.findViewById(R.id.scanAddress);
        mScanAddress.setOnClickListener(v -> {
            scanAddress();
        });

        mAddress = view.findViewById(R.id.address);
        mAmount = view.findViewById(R.id.amount);

        mSubmit = view.findViewById(R.id.submit);
        mSubmit.setOnClickListener(v -> {
            inputPinCode(false);
        });

        mSubmitWithToken = view.findViewById(R.id.submitWithToken);
        mSubmitWithToken.setOnClickListener(v -> {
            final String toAddress = mAddress.getText().toString();
            final String amount = mAmount.getText().toString();
            final Fee fee = (Fee) mFeeSpinner.getSelectedItem();
            createTransactionWithSecureToken(toAddress, amount, fee, true);

        });

        mLoading = view.findViewById(R.id.progress);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWithdrawViewModel = ViewModelProviders.of(this,
                new WithdrawViewModel.Factory(getActivity().getApplication(), mWallet))
                .get(WithdrawViewModel.class);

        mWithdrawViewModel.getUsage().observe(this, usage -> {
            if (usage == null) { // fetching
                mQuota.setText(getString(R.string.template_quota, "…", mWallet.currencySymbol));
                mUsage.setText(getString(R.string.template_usage, "…", mWallet.currencySymbol));
            } else {
                mQuota.setText(getString(R.string.template_quota, usage.dailyTransactionAmountQuota, mWallet.currencySymbol));
                mUsage.setText(getString(R.string.template_usage, usage.dailyTransactionAmountUsage, mWallet.currencySymbol));
            }
        });

        mWithdrawViewModel.getTransactionFee().observe(this, fees -> {
            mFeeAdapter.clear();
            mFeeAdapter.addAll(fees);
        });

        mViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);
        mViewModel.getBalance(mWallet).observe(this, entry -> {
            if (entry.init) {
                mBalance.setText(getString(R.string.template_amount,
                        CurrencyHelper.getEffectiveBalance(entry.balance), mWallet.currencySymbol));
            } else {
                mBalance.setText(getString(R.string.template_amount, "…", mWallet.currencySymbol));
            }
        });
        mViewModel.getCurrencies().observe(this, currencies -> {
            final Currency c = CurrencyHelper.findCurrency(currencies, mWallet);
            if (c != null) {
                mCurrency.setText(c.displayName);
            } else {
                mCurrency.setText(mWallet.currencySymbol);
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            mAddress.setText(result.getContents());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onPinCodeInput(String pinCode) {
        if (mPinForToken) {
            requestSecureToken(pinCode);
        } else {
            final String toAddress = mAddress.getText().toString();
            final String amount = mAmount.getText().toString();
            final Fee fee = (Fee) mFeeSpinner.getSelectedItem();
            createTransaction(toAddress, amount, fee, pinCode);
        }
    }

    @Override
    public void onForgotPinCode() {
        NavFragment.find(this).goRestore();
    }

    private void inputPinCode(boolean forToken) {
        mPinForToken = forToken;
        InputPinCodeDialog dialog = InputPinCodeDialog.newInstance();
        dialog.show(getChildFragmentManager(), "pinCode");
    }

    private void scanAddress() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.initiateScan();
    }

    private void setInProgress(boolean inProgress) {
        mAddress.setEnabled(!inProgress);
        mScanAddress.setEnabled(!inProgress);
        mAmount.setEnabled(!inProgress);
        mFeeSpinner.setEnabled(!inProgress);
        mSubmit.setEnabled(!inProgress);
        mLoading.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    }

    private void createTransaction(String toAddress, String amount, Fee fee, String pinCode) {
        if (toAddress.isEmpty() || amount.isEmpty() || fee == null || pinCode.isEmpty()) {
            return;
        }

        setInProgress(true);
        mService.createTransaction(mWallet.walletId, toAddress, amount, fee.amount, "", pinCode, new Callback<CreateTransactionResult>() {
            @Override
            public void onError(Throwable error) {
                mViewModel.getBalance(mWallet, true);
                Helpers.showToast(getContext(), "createTransaction failed: " + error.getMessage());
                setInProgress(false);
            }

            @Override
            public void onResult(CreateTransactionResult result) {
                getFragmentManager().popBackStack();
                refreshDetailHistory();
                setInProgress(false);
            }
        });
    }

    private void createTransactionWithSecureToken(String toAddress, String amount, Fee fee, boolean requestToken) {
        if (toAddress.isEmpty() || amount.isEmpty() || fee == null) {
            return;
        }

        setInProgress(true);
        mService.createTransaction(mWallet.walletId, toAddress, amount, fee.amount, "", new Callback<CreateTransactionResult>() {
            @Override
            public void onError(Throwable error) {
                if (requestToken && error instanceof Error && ((Error) error).getCode() == Error.Code.ErrUserSecureTokenNotReady) { // Secure token not ready
                    Helpers.showToast(getContext(), "Secure Token invalid/expired, input PIN code to request a new one");
                    onRequestSecureToken();
                } else {
                    mViewModel.getBalance(mWallet, true);
                    Helpers.showToast(getContext(), "createTransaction failed: " + error.getMessage());
                    setInProgress(false);
                }
            }

            @Override
            public void onResult(CreateTransactionResult result) {
                getFragmentManager().popBackStack();
                refreshDetailHistory();
                setInProgress(false);
            }
        });
    }

    private void onRequestSecureToken() {
        inputPinCode(true);
    }

    private void requestSecureToken(String pinCode) {
        final String toAddress = mAddress.getText().toString();
        final String amount = mAmount.getText().toString();
        final Fee fee = (Fee) mFeeSpinner.getSelectedItem();

        setInProgress(true);
        mService.requestSecureToken(pinCode, new Callback<RequestSecureTokenResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getContext(), "requestSecureToken failed: " + error.getMessage());
                setInProgress(false);
            }

            @Override
            public void onResult(RequestSecureTokenResult requestSecureTokenResult) { // retry transaction
                createTransactionWithSecureToken(toAddress, amount, fee, false);
            }
        });
    }

    private void refreshDetailHistory() {
        WalletDetailFragment wdf = NavFragment.find(this).findFragment(WalletDetailFragment.class);
        if (wdf != null) {
            wdf.refresh();
        }
    }
}

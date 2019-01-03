package com.cybavo.example.wallet.create;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cybavo.example.wallet.NavFragment;
import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.CurrencyHelper.Coin;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.pincode.InputPinCodeDialog;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.example.wallet.main.MainViewModel;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.CreateWalletResult;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

public class CreateWalletFragment extends Fragment implements InputPinCodeDialog.OnPinCodeInputListener {

    public static CreateWalletFragment newInstance() {
        CreateWalletFragment fragment = new CreateWalletFragment();
        return fragment;
    }

    private MainViewModel mViewModel;
    private MutableLiveData<Currency> mSelectedCurrency = new MutableLiveData<>();
    private MutableLiveData<Long> mParentWallet = new MutableLiveData<>();
    private MediatorLiveData<List<Wallet>> mParentWallets = new MediatorLiveData<>();

    private String mAccount = "";
    private String mName = "";

    private Spinner mCurrencySpinner;
    private CurrencyDropdownAdapter mCurrencyAdapter;

    private TextView mParentLabel;
    private Spinner mParentSpinner;
    private WalletDropdownAdapter mParentAdapter;

    private TextInputLayout mAccountInput;
    private EditText mNameEdit;
    private Button mSubmit;

    public CreateWalletFragment() {
        mParentWallet.setValue(0L);
        mSelectedCurrency.setValue(Currency.Unknown);
        mParentWallets.setValue(new ArrayList<>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_create_wallet)
                .onBack(v -> getFragmentManager().popBackStack())
                .done();

        mCurrencySpinner = view.findViewById(R.id.currency);
        mCurrencyAdapter = new CurrencyDropdownAdapter(view.getContext());
        mCurrencySpinner.setAdapter(mCurrencyAdapter);
        mCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Currency item = (Currency) parent.getAdapter().getItem(position);
                mSelectedCurrency.setValue(item);
            }
        });

        mParentLabel = view.findViewById(R.id.parentLabel);
        mParentSpinner = view.findViewById(R.id.parent);
        mParentAdapter = new WalletDropdownAdapter(view.getContext());
        mParentSpinner.setAdapter(mParentAdapter);
        mParentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Wallet item = (Wallet) parent.getAdapter().getItem(position);
                mParentWallet.setValue(item.walletId);
            }
        });

        mAccountInput = view.findViewById(R.id.accountWrapper);
        mAccountInput.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mAccount = s.toString();
            }
        });

        mNameEdit = view.findViewById(R.id.name);
        mNameEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mName = s.toString();
            }
        });

        mSubmit = view.findViewById(R.id.submit);
        mSubmit.setOnClickListener(v -> {
            inputPinCode();
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);

        mViewModel.getCreatableCurrencies().observe(this, currencies -> {
            mCurrencyAdapter.clear();
            mCurrencyAdapter.addAll(currencies);
        });

        mSelectedCurrency.observe(this, currency -> {
            if (currency.tokenAddress.isEmpty()) { // has no parent
                mParentLabel.setVisibility(View.GONE);
                mParentSpinner.setVisibility(View.GONE);
                mParentWallet.setValue(0L);
            } else {
                mParentLabel.setVisibility(View.VISIBLE);
                mParentSpinner.setVisibility(View.VISIBLE);
            }

            if (currency.currency == Coin.EOS) { // EOS has account
                mAccountInput.setVisibility(View.VISIBLE);
            } else {
                mAccountInput.setVisibility(View.GONE);
            }
        });

        mParentWallets.addSource(mViewModel.getWallets(), wallets -> {
            mParentWallets.setValue(calcParentWallets(wallets, mSelectedCurrency.getValue()));
        });
        mParentWallets.addSource(mSelectedCurrency, currency -> {
            mParentWallets.setValue(calcParentWallets(mViewModel.getWallets().getValue(), currency));
        });
        mParentWallets.observe(this, wallets -> {
            mParentAdapter.clear();
            mParentAdapter.addAll(wallets);
        });
    }

    private List<Wallet> calcParentWallets(List<Wallet> wallets, Currency currency) {
        List<Wallet> parents = new ArrayList<>();
        for (Wallet w : wallets) {
            if (currency.currency ==  w.currency && w.tokenAddress.isEmpty()) {
                parents.add(w);
            }
        }
        return parents;
    }

    private void setInProgress(boolean inProgress) {
        mCurrencySpinner.setEnabled(!inProgress);
        mParentSpinner.setEnabled(!inProgress);
        mAccountInput.setEnabled(!inProgress);
        mNameEdit.setEnabled(!inProgress);
        mSubmit.setEnabled(!inProgress);
    }

    private void inputPinCode() {
        InputPinCodeDialog dialog = InputPinCodeDialog.newInstance();
        dialog.show(getChildFragmentManager(), "pinCode");
    }

    @Override
    public void onPinCodeInput(String pinCode) {
        createWallet(pinCode);
    }

    @Override
    public void onForgotPinCode() {
        NavFragment.find(this).goRestore();
    }

    private void createWallet(String pinCode) {

        final Currency currency = mSelectedCurrency.getValue();
        final long parent = mParentWallet.getValue();

        if (currency == null || pinCode.isEmpty())
            return;

        Map<String, String> extras = new HashMap<>();
        if (currency.currency == Coin.EOS) {
            if (TextUtils.isEmpty(mAccount)) { // EOS must specify account
                return;
            }
            extras.put("account_name", mAccount);
        }

        setInProgress(true);
        Wallets.getInstance().createWallet(currency.currency, currency.tokenAddress, parent, mName, pinCode, extras, new Callback<CreateWalletResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getContext(), "createWallet failed: " + error.getMessage());
                setInProgress(false);
            }

            @Override
            public void onResult(CreateWalletResult result) {
                setInProgress(false);
                mViewModel.fetchWallets();
                getFragmentManager().popBackStack();
            }
        });
    }
}

package com.cybavo.example.wallet.detail;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.CurrencyHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.example.wallet.main.MainViewModel;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Wallet;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class DepositFragment extends Fragment {

    private static final String TAG = DepositFragment.class.getSimpleName();
    private static final String ARG_WALLET = "wallet";
    private static final int SAVE_QR_CODE_REQUEST_CODE = 43;

    private Wallet mWallet;
    private MainViewModel mViewModel;

    private TextView mCurrency;
    private TextView mWarning;
    private TextView mAddress;
    private ImageView mQrCode;
    private Button mCopyAddress;
    private Button mSaveQrCode;

    private Bitmap mQrCodeImage;

    public static DepositFragment newInstance(Wallet wallet) {
        DepositFragment fragment = new DepositFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLET, WalletParcelable.fromWallet(wallet));
        fragment.setArguments(args);
        return fragment;
    }

    public DepositFragment() {
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
        return inflater.inflate(R.layout.fragment_deposit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_deposit)
                .onBack(v -> getFragmentManager().popBackStack())
                .done();

        mCurrency = view.findViewById(R.id.currency);
        mCurrency.setText(mWallet.currencySymbol);
        mCurrency.setCompoundDrawablesWithIntrinsicBounds(
                CurrencyHelper.getCoinIconResource(getContext(), mWallet.currencySymbol), 0, 0, 0);

        mWarning = view.findViewById(R.id.warning);
        mWarning.setText(getString(R.string.warning_withdraw, mWallet.currencySymbol));

        mAddress = view.findViewById(R.id.address);
        mAddress.setText(mWallet.address);

        mQrCode = view.findViewById(R.id.qrCode);
        mQrCodeImage = Helpers.createQrCodeBitmap(mWallet.address,
                getResources().getInteger(R.integer.qr_code_image_size));
        mQrCode.setImageBitmap(mQrCodeImage);

        mCopyAddress = view.findViewById(R.id.copyAddress);
        mCopyAddress.setOnClickListener(v -> {
            copyAddress();
        });
        mSaveQrCode = view.findViewById(R.id.saveQrCode);
        mSaveQrCode.setOnClickListener(v -> {
            saveQrCode();
        });
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SAVE_QR_CODE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                new SaveImageTask(getContext().getApplicationContext()).execute(
                        Pair.create(mQrCodeImage, uri)
                );
            }
        }
    }

    private void saveQrCode() {
        final String fileName = String.format(Locale.getDefault(), "%s-%s",
                mWallet.currencySymbol, mWallet.address);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, SAVE_QR_CODE_REQUEST_CODE);
    }

    private void copyAddress() {
        ClipData cd = ClipData.newPlainText(
                getString(R.string.label_clipboard_address, mWallet.currencySymbol), mWallet.address);
        final ClipboardManager clipboard = (ClipboardManager) getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(cd);
        Toast.makeText(getContext(), R.string.message_address_copied, Toast.LENGTH_SHORT).show();
    }
}

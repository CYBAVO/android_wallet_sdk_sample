package com.cybavo.example.wallet.detail;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.cybavo.example.wallet.R;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.RenameWalletResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class RenameWalletDialog extends DialogFragment {

    public interface OnRenameListener {
        void onRename(String name);
    }

    private static final String TAG = RenameWalletDialog.class.getSimpleName();
    private static final String ARG_WALLET = "wallet";
    private Wallet mWallet;

    static RenameWalletDialog newInstance(Wallet wallet) {
        RenameWalletDialog dialog = new RenameWalletDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLET, WalletParcelable.fromWallet(wallet));
        dialog.setArguments(args);
        return dialog;
    }

    public RenameWalletDialog() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWallet = WalletParcelable.toWallet(getArguments().getParcelable(ARG_WALLET));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View view = inflater.inflate(R.layout.dialog_rename_wallet, null, false);
        final EditText nameEdit = view.findViewById(R.id.name);
        nameEdit.setText(mWallet.name);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.title_rename_wallet)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (getParentFragment() instanceof OnRenameListener) {
                        ((OnRenameListener) getParentFragment()).onRename(nameEdit.getText().toString());
                    }
                })
                .create();
    }
}

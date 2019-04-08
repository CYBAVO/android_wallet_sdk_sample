/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.cybavo.example.wallet.create.CreateWalletFragment;
import com.cybavo.example.wallet.detail.DepositFragment;
import com.cybavo.example.wallet.detail.TransactionDetailFragment;
import com.cybavo.example.wallet.detail.WalletDetailFragment;
import com.cybavo.example.wallet.detail.WithdrawFragment;
import com.cybavo.example.wallet.pincode.ChangePinFragment;
import com.cybavo.example.wallet.pincode.RecoverPinFragment;
import com.cybavo.example.wallet.pincode.RestorePinFragment;
import com.cybavo.example.wallet.pincode.SetupFragment;
import com.cybavo.example.wallet.settings.SettingsFragment;
import com.cybavo.wallet.service.wallet.Transaction;
import com.cybavo.wallet.service.wallet.Wallet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class NavFragment extends Fragment implements FragmentManager.OnBackStackChangedListener {

    public static NavFragment newInstance() {
        return new NavFragment();
    }

    public static NavFragment find(Fragment fragment) {
        if (fragment == null) {
            return  null;
        }
        if (fragment instanceof NavFragment) {
            return (NavFragment) fragment;
        }
        return find(fragment.getParentFragment());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nav, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getChildFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getChildFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onBackStackChanged() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    public <F extends Fragment> F findFragment(Class<F> clz) {
        return (F) getChildFragmentManager().findFragmentByTag(clz.getSimpleName());
    }

    public void goSetup() {
        if (fragmentExists(SetupFragment.class))
            return;

        showFragment(SetupFragment.newInstance(), false); // don't allow back
    }

    public void leaveSetup() {
        if (!fragmentExists(SetupFragment.class)) {
            return;
        }

        final Fragment fragment = getChildFragmentManager()
                .findFragmentByTag(SetupFragment.class.getSimpleName());
        if (fragment != null) {
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
        }
    }

    public void goRestore() {
        if (fragmentExists(RestorePinFragment.class))
            return;

        showFragment(RestorePinFragment.newInstance(), true);
    }

    public void goChangePin() {
        if (fragmentExists(ChangePinFragment.class))
            return;

        showFragment(ChangePinFragment.newInstance(), true);
    }

    public void goRecoverPin() {
        if (fragmentExists(RecoverPinFragment.class))
            return;

        showFragment(RecoverPinFragment.newInstance(), true);
    }

    public void goCreateWallet() {
        if (fragmentExists(CreateWalletFragment.class))
            return;

        showFragment(CreateWalletFragment.newInstance(), true);
    }

    public void goWalletDetail(Wallet wallet) {
        if (fragmentExists(WalletDetailFragment.class))
            return;

        showFragment(WalletDetailFragment.newInstance(wallet), true);
    }

    public void goTransactionDetail(Wallet wallet, Transaction transaction) {
        if (fragmentExists(TransactionDetailFragment.class))
            return;

        showFragment(TransactionDetailFragment.newInstance(wallet, transaction), true);
    }

    public void goDeposit(Wallet wallet) {
        if (fragmentExists(DepositFragment.class))
            return;

        showFragment(DepositFragment.newInstance(wallet), true);
    }

    public void goWithdraw(Wallet wallet) {
        if (fragmentExists(WithdrawFragment.class))
            return;

        showFragment(WithdrawFragment.newInstance(wallet), true);
    }

    public void goSettings() {
        if (fragmentExists(SettingsFragment.class))
            return;

        showFragment(SettingsFragment.newInstance(), true);
    }

    public <F extends Fragment> boolean fragmentExists(Class<F> clz) {
        return findFragment(clz) != null;
    }

    private void showFragment(Fragment instance, boolean backStack) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction()
                .add(R.id.navRoot, instance, instance.getClass().getSimpleName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (backStack) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }
}

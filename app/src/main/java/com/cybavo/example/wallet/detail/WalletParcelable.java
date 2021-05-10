/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;

import android.os.Parcel;
import android.os.Parcelable;

import com.cybavo.wallet.service.wallet.Wallet;

public class WalletParcelable implements Parcelable {

    public static WalletParcelable fromWallet(Wallet wallet) {
        return new WalletParcelable(wallet);
    }

    public static Wallet toWallet(Parcelable parcelable) {
        return ((WalletParcelable) parcelable).wallet;
    }

    final Wallet wallet;

    private WalletParcelable(Wallet wallet) {
        this.wallet = wallet;
    }

    protected WalletParcelable(Parcel in) {
        final Wallet wallet = new Wallet();
        wallet.walletId = in.readLong();
        wallet.type = in.readInt();
        wallet.address = in.readString();
        wallet.name = in.readString();
        wallet.currency = in.readLong();
        wallet.tokenAddress = in.readString();
        wallet.currencySymbol = in.readString();
        wallet.parentWalletId = in.readLong();
        this.wallet = wallet;
    }

    public static final Creator<WalletParcelable> CREATOR = new Creator<WalletParcelable>() {
        @Override
        public WalletParcelable createFromParcel(Parcel in) {
            return new WalletParcelable(in);
        }

        @Override
        public WalletParcelable[] newArray(int size) {
            return new WalletParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(wallet.walletId);
        dest.writeInt(wallet.type);
        dest.writeString(wallet.address);
        dest.writeString(wallet.name);
        dest.writeLong(wallet.currency);
        dest.writeString(wallet.tokenAddress);
        dest.writeString(wallet.currencySymbol);
        dest.writeLong(wallet.parentWalletId);
    }
}

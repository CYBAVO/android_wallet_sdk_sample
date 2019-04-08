/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.config;

import android.content.Context;

import com.cybavo.example.wallet.R;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Wallet;

import java.util.List;

import androidx.preference.PreferenceManager;

public class Config {

    public final static String PREFKEY_ENDPOINT = "pref_endpoint";
    public final static String PREFKEY_API_CODE = "pref_api_code";

    /**
     * 1 wallet per currency
     */
    private final static int WALLET_LIMIT_PER_CURRENCY = 1;

    /**
     *  An implementation reference to control wallet creation
     */
    public static boolean canCreateWallet(List<Wallet> existing, Currency currency) {
        int count = 0;
        for (Wallet w : existing) {
            if (w.currency == currency.currency &&
                    w.tokenAddress.equals(currency.tokenAddress)) {
                count++;
            }
        }
        return (count < Config.WALLET_LIMIT_PER_CURRENCY);
    }

    public static String getEndpoint(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_ENDPOINT,
                context.getString(R.string.default_endpoint));
    }

    public static String getApiCode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_API_CODE,
                context.getString(R.string.default_api_code));
    }
}

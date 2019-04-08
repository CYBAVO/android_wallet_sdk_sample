/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet;

import android.app.Application;
import android.util.Log;

import com.cybavo.example.wallet.config.Config;
import com.cybavo.wallet.service.WalletSdk;

public class WalletApplication extends Application {

    private static final String TAG = WalletApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        // initialize Wallets SDK
        final String endpoint = Config.getEndpoint(this);
        final String apiCode = Config.getApiCode(this);
        WalletSdk.init(getApplicationContext(), new WalletSdk.Configuration(
                endpoint, apiCode
        ));
        Log.d(TAG, "Wallet SDK endpoint: " + endpoint);
    }
}

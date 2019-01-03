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
        WalletSdk.init(getApplicationContext(), new WalletSdk.Configuration(
                endpoint
        ));
        Log.d(TAG, "Wallet SDK endpoint: " + endpoint);
    }
}

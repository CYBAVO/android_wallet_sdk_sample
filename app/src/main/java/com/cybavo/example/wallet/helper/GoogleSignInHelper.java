package com.cybavo.example.wallet.helper;

import android.content.Context;

import com.cybavo.example.wallet.BuildConfig;
import com.cybavo.example.wallet.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class GoogleSignInHelper {

    public static GoogleSignInClient sClient;
    public static GoogleSignInClient getClient(Context context) {
        if (sClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.google_sign_in_web_cli_id))
                    .requestEmail()
                    .requestProfile()
                    .build();
            sClient = GoogleSignIn.getClient(context, gso);
        }
        return sClient;
    }
}

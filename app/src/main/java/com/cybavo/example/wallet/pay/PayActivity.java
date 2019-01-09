package com.cybavo.example.wallet.pay;

import android.os.Bundle;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.auth.GoogleSignInFragment;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.SignInState;
import com.cybavo.wallet.service.auth.SignInStateListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class PayActivity extends AppCompatActivity implements SignInStateListener {

    private final static String TAG = PayActivity.class.getSimpleName();

    public static final String ARG_PAY_CURRENCY = "pay_currency";
    public static final String ARG_PAY_TOKEN_ADDRESS = "pay_token_address";
    public static final String ARG_PAY_AMOUNT = "pay_amount";
    public static final String ARG_PAY_TARGET_ADDRESS = "pay_target_address";

    private Auth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);

        mAuth = Auth.getInstance();
        mAuth.addSignInStateListener(this);

        if (savedInstanceState == null) { // or fragment will restore itself
            handleSignInStateChange(mAuth.getSignInState());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuth.removeSignInStateListener(this);
        mAuth = null;
    }

    @Override
    public void onUserStateChanged(SignInState state) {
        handleSignInStateChange(state);
    }

    private void handleSignInStateChange(SignInState state) {
        switch (state) {
            case SIGNED_IN:
                showPay();
                break;
            case SIGNED_OUT:
                showSignIn();
                break;
            case SESSION_EXPIRED:
                signOut();
                break;
        }
    }

    private void showSignIn() {

        final FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentByTag(GoogleSignInFragment.class.getSimpleName()) == null) {
            fm.beginTransaction().replace(R.id.fragmentRoot, GoogleSignInFragment.newInstance(), GoogleSignInFragment.class.getSimpleName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    }

    private void showPay() {

        final int currency = getIntent().getIntExtra(ARG_PAY_CURRENCY, -1);
        final String tokenAddress = getIntent().getStringExtra(ARG_PAY_TOKEN_ADDRESS);
        final String amount = getIntent().getStringExtra(ARG_PAY_AMOUNT);
        final String targetAddress = getIntent().getStringExtra(ARG_PAY_TARGET_ADDRESS);

        final FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(PayFragment.class.getSimpleName()) == null) {
            fm.beginTransaction().replace(R.id.fragmentRoot, PayFragment.newInstance(currency, tokenAddress, amount, targetAddress), PayFragment.class.getSimpleName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
        }
    }
    private void signOut() {
        mAuth.signOut();
    }
}

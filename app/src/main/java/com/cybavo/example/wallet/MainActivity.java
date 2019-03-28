package com.cybavo.example.wallet;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.cybavo.example.wallet.auth.GoogleSignInFragment;
import com.cybavo.example.wallet.helper.GoogleSignInHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.SignInState;
import com.cybavo.wallet.service.auth.SignInStateListener;
import com.cybavo.wallet.service.auth.results.SignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity implements SignInStateListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    private Auth mAuth;
    private View mRenewSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRenewSession = findViewById(R.id.renewSession);

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

    // handle child fragment backstack
    private boolean onBackPressed(FragmentManager fm) {
        if (fm != null) {
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
                return true;
            }

            List<Fragment> fragList = fm.getFragments();
            if (fragList.size() > 0) {
                for (Fragment frag : fragList) {
                    if (frag == null) {
                        continue;
                    }
                    if (frag.isVisible()) {
                        if (onBackPressed(frag.getChildFragmentManager())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (onBackPressed(fm)) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onSignInStateChanged(SignInState state) {
        handleSignInStateChange(state);
    }

    private void handleSignInStateChange(SignInState state) {
        switch (state) {
            case SIGNED_IN:
                showMain();
                break;
            case SIGNED_OUT:
                showSignIn();
                break;
            case SESSION_EXPIRED:
                renewSession();
                break;
            case SESSION_INVALID:
                signOut();
                break;
        }
    }

    private void showRenewSession(boolean visible) {
        mRenewSession.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void showSignIn() {
        showRenewSession(false);

        final FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentByTag(GoogleSignInFragment.class.getSimpleName()) == null) {
            fm.beginTransaction().replace(R.id.fragmentRoot, GoogleSignInFragment.newInstance(), GoogleSignInFragment.class.getSimpleName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    }

    private void showMain() {
        showRenewSession(false);

        final FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(NavFragment.class.getSimpleName()) == null) {
            fm.beginTransaction().replace(R.id.fragmentRoot, NavFragment.newInstance(), NavFragment.class.getSimpleName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
        }
    }

    private void renewSession() {
        showRenewSession(true);

        GoogleSignInClient cli = GoogleSignInHelper.getClient(getApplicationContext());
        Task<GoogleSignInAccount> task = cli.silentSignIn();
        if (task.isSuccessful()) {
            signIn(task.getResult());
        } else {
            task.addOnCompleteListener(this, task1 -> {
                try {
                    GoogleSignInAccount signInAccount = task.getResult(ApiException.class);
                    signIn(signInAccount);
                } catch (ApiException e) {
                    Log.e(TAG, "silentSignIn failed", e);
                    Helpers.showToast(MainActivity.this, "Google sign in failed: " + e.getMessage());
                    signOut();
                }
            });
        }
    }

    private void signIn(GoogleSignInAccount account) {
        mAuth.signIn(account.getIdToken(), "Google", new Callback<SignInResult>() {
            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "signIn failed", error);
                Helpers.showToast(getApplicationContext(), "signIn failed: " + error.getMessage());
                signOut();
            }

            @Override
            public void onResult(SignInResult signInResult) {
                Helpers.showToast(getApplicationContext(), getString(R.string.message_renew_session_success));
            }
        });
    }

    private void signOut() {
        mAuth.signOut();
    }
}

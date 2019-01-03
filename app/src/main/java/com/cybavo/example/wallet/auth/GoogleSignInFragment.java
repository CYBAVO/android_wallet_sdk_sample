package com.cybavo.example.wallet.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.GoogleSignInHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.settings.SettingsFragment;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.api.Error;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.results.SignInResult;
import com.cybavo.wallet.service.auth.results.SignUpResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


public class GoogleSignInFragment extends Fragment {

    private static final String TAG = GoogleSignInFragment.class.getSimpleName();
    private static final int GOOGLE_SIGN_IN_REQUEST_CODE = 43;

    public GoogleSignInFragment() {
        // Required empty public constructor
    }

    public static GoogleSignInFragment newInstance() {
        return new GoogleSignInFragment();
    }

    private Auth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private SignInButton mSignIn;
    private ProgressBar mProgress;
    private View mSettings;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_google_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = Auth.getInstance();

        mGoogleSignInClient = GoogleSignInHelper.getClient(view.getContext());

        mProgress = view.findViewById(R.id.progress);
        mSignIn = view.findViewById(R.id.googleSignIn);
        mSignIn.setSize(SignInButton.SIZE_WIDE);
        mSignIn.setOnClickListener(v -> performSignIn());

        mSettings = view.findViewById(R.id.action_settings);
        mSettings.setOnClickListener(v -> goSettings());
    }

    private void goSettings() {
        FragmentTransaction ft = getFragmentManager().beginTransaction()
                .add(R.id.fragmentRoot, SettingsFragment.newInstance(), SettingsFragment.class.getSimpleName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("");
        ft.commit();
    }

    private void performSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE);
        setInProgress(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                signInWithGoogle(account);
            } catch (ApiException e) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.e(TAG, "Google sign in failed: " + e.getStatusCode(), e);
                Helpers.showToast(getContext(), "Google sign in failed: " + e.getStatusCode());
                setInProgress(false);
            }
        }
    }

    void setInProgress(boolean inProgress) {
        mSignIn.setEnabled(!inProgress);
        mProgress.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    }

    private void signInWithGoogle(GoogleSignInAccount account) {
        setInProgress(true);
        mAuth.signIn(account.getIdToken(), new Callback<SignInResult>() {
            @Override
            public void onError(Throwable error) {
                if (error instanceof Error && ((Error)error).getCode() == Error.Code.ErrRegistrationRequired) { // registration required
                    registerWithGoogle(account);
                } else { // sign in failed
                    onSignInFailed(error);
                }
            }

            @Override
            public void onResult(SignInResult result) {
                setInProgress(false);
            }
        });
    }

    private void registerWithGoogle(GoogleSignInAccount account) {
        setInProgress(true);
        mAuth.signUp(account.getIdToken(), new Callback<SignUpResult>() {
            @Override
            public void onError(Throwable error) {
                onSignInFailed(error);
            }

            @Override
            public void onResult(SignUpResult result) { // sign up success, retry sign in
                signInWithGoogle(account);
            }
        });
    }

    private void onSignInFailed(Throwable error) {
        Helpers.showToast(getContext(), "Sign in failed: " + error.getMessage());
        mGoogleSignInClient.signOut().addOnCompleteListener(getActivity(), task -> {
            setInProgress(false);
        });
    }
}

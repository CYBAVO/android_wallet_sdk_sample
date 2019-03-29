package com.cybavo.example.wallet.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.GoogleSignInHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.settings.SettingsFragment;
import com.cybavo.example.wallet.wxapi.WXEntryActivity;
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

import static com.cybavo.example.wallet.Constants.ID_PROVIDER_GOOGLE;
import static com.cybavo.example.wallet.Constants.ID_PROVIDER_WECHAT;


public class SignInFragment extends Fragment {

    private static final String TAG = SignInFragment.class.getSimpleName();
    private static final int GOOGLE_SIGN_IN_REQUEST_CODE = 43;
    private static final int WECHAT_SIGN_IN_REQUEST_CODE = 44;

    public SignInFragment() {
        // Required empty public constructor
    }

    public static SignInFragment newInstance() {
        return new SignInFragment();
    }

    private Auth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private SignInButton mGoogleSignIn;
    private Button mWeChatSignIn;
    private ProgressBar mProgress;
    private View mSettings;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = Auth.getInstance();

        mGoogleSignInClient = GoogleSignInHelper.getClient(view.getContext());

        mProgress = view.findViewById(R.id.progress);
        mGoogleSignIn = view.findViewById(R.id.googleSignIn);
        mGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
        mGoogleSignIn.setOnClickListener(v -> performSignIn(ID_PROVIDER_GOOGLE));

        mWeChatSignIn = view.findViewById(R.id.weChatSignIn);
        mWeChatSignIn.setOnClickListener(v -> performSignIn(ID_PROVIDER_WECHAT));

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

    private void performSignIn(String identityProvider) {
        if (ID_PROVIDER_GOOGLE.equals(identityProvider)) {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE);
        }
        else { // ID_PROVIDER_WECHAT
            Intent intent = new Intent(getContext(), WXEntryActivity.class);
            intent.setAction("sign_in");
            startActivityForResult(intent, WECHAT_SIGN_IN_REQUEST_CODE);
        }
        setInProgress(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                signInWithToken(account.getIdToken(), ID_PROVIDER_GOOGLE, new Identity(ID_PROVIDER_GOOGLE, account.getDisplayName(), account.getEmail(), account.getPhotoUrl().toString()), true
                );
            } catch (ApiException e) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.e(TAG, "Google sign in failed: " + e.getStatusCode(), e);
                Helpers.showToast(getContext(), "Google sign in failed: " + e.getStatusCode());
                setInProgress(false);
            }
        } else if (requestCode == WECHAT_SIGN_IN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                signInWithToken(data.getStringExtra("code"), ID_PROVIDER_WECHAT, new Identity(ID_PROVIDER_WECHAT, "WeChat user", "No email", ""), true
                );
            } else {
                Helpers.showToast(getContext(), "WeChat sign in failed: (" + data.getIntExtra("errCode", -1) + ")");
                setInProgress(false);
            }
        }
    }

    void setInProgress(boolean inProgress) {
        mGoogleSignIn.setEnabled(!inProgress);
        mWeChatSignIn.setEnabled(!inProgress);
        mProgress.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    }

    private void signInWithToken(String token, String identityProvider, Identity identity, boolean autoRegister) {
        setInProgress(true);
        mAuth.signIn(token, identityProvider, new Callback<SignInResult>() {
            @Override
            public void onError(Throwable error) {
                if (autoRegister && error instanceof Error && ((Error)error).getCode() == Error.Code.ErrRegistrationRequired) { // registration required
                    registerWithToken(token, identityProvider, identity);
                } else { // sign in failed
                    onSignInFailed(error);
                }
            }

            @Override
            public void onResult(SignInResult result) {
                setInProgress(false);
                identity.save(getContext());
            }
        });
    }

    private void registerWithToken(String token, String identityProvider, Identity identity) {
        setInProgress(true);
        mAuth.signUp(token, identityProvider, new Callback<SignUpResult>() {
            @Override
            public void onError(Throwable error) {
                onSignInFailed(error);
            }

            @Override
            public void onResult(SignUpResult result) { // sign up success, retry sign in
                signInWithToken(token, identityProvider, identity, false);
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

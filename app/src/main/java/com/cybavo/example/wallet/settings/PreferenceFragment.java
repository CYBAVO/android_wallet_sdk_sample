package com.cybavo.example.wallet.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cybavo.example.wallet.BuildConfig;
import com.cybavo.example.wallet.NavFragment;
import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.config.Config;
import com.cybavo.example.wallet.helper.GoogleSignInHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.WalletSdk;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.SignInState;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

import java.util.Arrays;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static class SignOutDialog extends DialogFragment {

        public SignOutDialog() {
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setTitle(R.string.title_confirm_sign_out)
                    .setMessage(R.string.message_confirm_sign_out)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.action_sign_out, (dialog, which) -> {
                        signOut(activity);
                    })
                    .create();
        }

        private void signOut(Activity activity) {
            GoogleSignInClient cli = GoogleSignInHelper.getClient(activity);
            cli.signOut().addOnCompleteListener(activity, task -> {
                Auth.getInstance().signOut();
            });
        }
    }
    private final static String KEY_ACCOUNT = "action_account";
    private final static String KEY_CHANGE_PIN = "action_change_pin";
    private final static String KEY_RESTORE_PIN = "action_restore_pin";
    private final static String KEY_SDK_VER = "pref_sdk_version";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (Auth.getInstance().getSignInState() != SignInState.SIGNED_OUT) {
            addPreferencesFromResource(R.xml.preference);
        }
        if (BuildConfig.DEBUG) {
            addPreferencesFromResource(R.xml.preference_dev);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Context context = view.getContext();

        // account
        final Preference accountPref = findPreference(KEY_ACCOUNT);
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (accountPref != null && account != null) {
            accountPref.setEnabled(true);
            accountPref.setTitle(account.getDisplayName());
            accountPref.setSummary(account.getEmail());
            // load profile image
            Glide.with(view)
                    .asBitmap()
                    .load(account.getPhotoUrl())
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
                            drawable.setCircular(true);
                            accountPref.setIcon(drawable);
                        }
                    });
        }
        // dev
        final Preference sdkVerPref = findPreference(KEY_SDK_VER);
        if (sdkVerPref != null) {
            Map<String, String> info = WalletSdk.getSDKInfo();
            sdkVerPref.setSummary(
                    String.format("%s (%s) - %s",
                            info.get("VERSION_NAME"),
                            info.get("VERSION_CODE"),
                            info.get("BUILD_TYPE"))
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case KEY_ACCOUNT:
                confirmSignOut();
                return true;
            case KEY_CHANGE_PIN:
                goChangePin();
                return true;
            case KEY_RESTORE_PIN:
                goRestorePin();
                return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    final String[] RESTART = new String[]{ Config.PREFKEY_ENDPOINT, Config.PREFKEY_API_CODE };
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Arrays.asList(RESTART).contains(key)) {
            Helpers.showToast(getContext(), getString(R.string.message_change_restart));
        }
    }

    private void setPreferenceVisible(String key, boolean visible) {
        final Preference prefs = findPreference(key);
        if (prefs != null) {
            prefs.setVisible(visible);
        }
    }

    private void confirmSignOut() {
        final String tag = SignOutDialog.class.getSimpleName();
        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            new SignOutDialog().show(getChildFragmentManager(), tag);
        }
    }

    private void goChangePin() {
        NavFragment.find(this).goChangePin();
    }

    private void goRestorePin() {
        NavFragment.find(this).goRestore();
    }
}

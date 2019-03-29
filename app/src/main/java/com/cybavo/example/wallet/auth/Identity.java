package com.cybavo.example.wallet.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import androidx.preference.PreferenceManager;

public class Identity {
    @SerializedName("provider")
    public String provider = "";
    @SerializedName("name")
    public String name = "";
    @SerializedName("email")
    public String email = "";
    @SerializedName("avatar")
    public String avatar = "";

    public Identity(String provider, String name, String email, String avatar) {
        this.provider = provider;
        this.name = name;
        this.email = email;
        this.avatar = avatar;
    }

    private static final String EMPTY_JSON = new Gson().toJson(new Identity("", "", "", ""));
    private static final String PREFKEY_IDENTITY = "identity";

    public Identity save(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREFKEY_IDENTITY, new Gson().toJson(this))
                .apply();
        return this;
    }

    public static void clear(Context context) {
        new Identity("", "", "", "")
                .save(context);
    }

    public static Identity read(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String json = prefs.getString(PREFKEY_IDENTITY, EMPTY_JSON);
        return new Gson().fromJson(json, Identity.class);
    }
}

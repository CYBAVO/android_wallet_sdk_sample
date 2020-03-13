/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cybavo.example.wallet.R;
import com.cybavo.wallet.service.wallet.Fee;

public class TokenIdAdapter extends ArrayAdapter<String> {

    public TokenIdAdapter(@NonNull Context context) {
        super(context, R.layout.dropdown_item_token_id);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dropdown_item_token_id, parent, false);
        }

        TextView amount = convertView.findViewById(R.id.amount);

        String tokenId = getItem(position);
        amount.setText(tokenId);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}

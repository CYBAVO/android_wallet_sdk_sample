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

import com.cybavo.example.wallet.R;
import com.cybavo.wallet.service.wallet.Fee;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FeeAdapter extends ArrayAdapter<Fee> {

    public FeeAdapter(@NonNull Context context) {
        super(context, R.layout.dropdown_item_fee);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dropdown_item_fee, parent, false);
        }

        TextView amount = convertView.findViewById(R.id.amount);
        TextView description = convertView.findViewById(R.id.description);

        Fee fee = getItem(position);
        amount.setText(fee.amount);
        description.setText(fee.description);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}

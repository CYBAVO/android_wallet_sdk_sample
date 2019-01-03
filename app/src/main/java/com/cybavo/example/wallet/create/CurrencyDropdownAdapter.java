package com.cybavo.example.wallet.create;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.CurrencyHelper;
import com.cybavo.wallet.service.wallet.Currency;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CurrencyDropdownAdapter extends ArrayAdapter<Currency> {

    public CurrencyDropdownAdapter(@NonNull Context context) {
        super(context, R.layout.dropdown_item_currency);
    }

    private View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent, boolean showSymbol) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dropdown_item_currency, parent, false);
        }

        Currency currency = getItem(position);

        ImageView icon = convertView.findViewById(R.id.icon);
        TextView name = convertView.findViewById(R.id.name);
        TextView symbol = convertView.findViewById(R.id.symbol);

        icon.setImageResource(CurrencyHelper.getCoinIconResource(parent.getContext(), currency.symbol));
        name.setText(currency.displayName);
        symbol.setText(currency.symbol);
        symbol.setVisibility(showSymbol ? View.VISIBLE : View.GONE);

        return convertView;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent, true);
    }
}

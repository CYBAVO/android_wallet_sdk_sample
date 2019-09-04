/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.wallet.service.wallet.Transaction;
import com.cybavo.wallet.service.wallet.Wallet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.ViewHolder> {

    public interface HistoryListListener {
        void onClickTransaction(Transaction item);
    }
    private final HistoryListListener mListener;
    private final List<Transaction> mItems = new ArrayList<>();
    private final Wallet mWallet;

    public HistoryListAdapter(Wallet wallet, HistoryListListener listener) {
        this.mWallet = wallet;
        this.mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Transaction item = mItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void updateTransactions(List<Transaction> transactions) {
        mItems.clear();
        mItems.addAll(transactions);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView operation;
        TextView amount;
        TextView time;
        TextView txid;
        ImageView failed;

        private Transaction mItem;

        ViewHolder(View view) {
            super(view);
            operation = view.findViewById(R.id.operation);
            amount = view.findViewById(R.id.amount);
            time = view.findViewById(R.id.time);
            txid = view.findViewById(R.id.txid);
            failed = view.findViewById(R.id.failed);
        }

        void bind(Transaction item) {
            this.mItem = item;

            operation.setText(item.direction == Transaction.Direction.IN ?
                R.string.label_deposit : R.string.label_withdraw);
            final int color = itemView.getContext().getResources().getColor(item.direction == Transaction.Direction.IN ?
                    android.R.color.holo_green_light : android.R.color.holo_orange_light
            );
            DrawableCompat.setTint(operation.getBackground(), color);

            amount.setText(item.amount);

            txid.setText(item.txid);

            time.setText(DateFormat.getDateFormat(itemView.getContext()).format(new Date(item.timestamp * 1000)));

            itemView.setAlpha(item.pending ? .5f : 1f);

            failed.setVisibility(item.success ? View.GONE : View.VISIBLE);

            // onClick event
            itemView.setOnClickListener(v -> {
                if (null != mListener) {
                    mListener.onClickTransaction(mItem);
                }
            });
        }
    }
}

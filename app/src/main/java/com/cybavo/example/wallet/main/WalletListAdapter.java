package com.cybavo.example.wallet.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.CurrencyHelper;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Wallet;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

public class WalletListAdapter extends RecyclerView.Adapter<WalletListAdapter.ViewHolder> {

    public interface WalletListListener {
        void onClickWallet(Wallet item);
    }
    private final LifecycleOwner mLifecycleOwner;
    private final MainViewModel mViewModel;
    private final WalletListListener mListener;
    private final List<Wallet> mItems = new ArrayList<>();
    private List<Currency> mCurrencies;

    public WalletListAdapter(LifecycleOwner lifecycleOwner, MainViewModel viewModel, WalletListListener listener) {
        mLifecycleOwner = lifecycleOwner;
        mViewModel = viewModel;
        mListener = listener;
        mViewModel.getCurrencies().observe(mLifecycleOwner, currencies -> {
            mCurrencies = new ArrayList<>(currencies);
            notifyDataSetChanged();
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_wallet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Wallet item = mItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void updateWallets(List<Wallet> wallets) {
        mItems.clear();
        mItems.addAll(wallets);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
        implements Observer<BalanceEntry> {

        ImageView icon;
        TextView currency;
        TextView balance;
        TextView name;

        private Wallet mItem;


        ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.icon);
            name = view.findViewById(R.id.name);
            balance = view.findViewById(R.id.balance);
            currency = view.findViewById(R.id.currency);
        }

        void bind(Wallet item) {
            if (mItem != null) {
                mViewModel.getBalance(mItem).removeObserver(this);
            }
            this.mItem = item;

            // coin icon
            icon.setImageResource(CurrencyHelper.getCoinIconResource(
                    itemView.getContext(), item.currencySymbol));
            currency.setText(item.currencySymbol);

            // wallet name
            if (item.name.isEmpty()) {
                name.setText(R.string.label_unnamed_wallet);
                name.setAlpha(.33f);
            } else {
                name.setText(item.name);
                name.setAlpha(1f);
            }

            final Currency c = CurrencyHelper.findCurrency(mCurrencies, item);
            currency.setText(c != null ? c.displayName : item.currencySymbol);

            // observe balance
            mViewModel.getBalance(item).observe(mLifecycleOwner, this);

            // onClick event
            itemView.setOnClickListener(v -> {
                if (null != mListener) {
                    mListener.onClickWallet(mItem);
                }
            });
        }

        @Override
        public void onChanged(BalanceEntry entry) {
            this.balance.setText(entry.init ? entry.balance.balance : "…");
        }
    }
}

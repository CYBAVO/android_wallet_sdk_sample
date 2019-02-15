package com.cybavo.example.wallet.helper;

import android.content.Context;

import com.cybavo.wallet.service.wallet.Balance;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Wallet;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.DrawableRes;
import androidx.core.util.Pair;

public class CurrencyHelper {

    // known coin types
    public interface Coin {
        int BTC = 0;
        int LTC = 2;
        int ETH = 60;
        int XRP = 144;
        int BCH = 145;
        int EOS = 194;
        int TRX = 195;
    }

    // Block explorer
    private final static Map<Pair<Integer, String>, String> sTxExplorers = new HashMap<>();
    static {
        CurrencyHelper.sTxExplorers.put(Pair.create(Coin.BTC, ""), "https://blockexplorer.com/tx/%s"); // BTC
        CurrencyHelper.sTxExplorers.put(Pair.create(Coin.BTC, "31"), "https://omniexplorer.info/tx/%s"); // USDT-Omni
        CurrencyHelper.sTxExplorers.put(Pair.create(Coin.LTC, ""), "https://live.blockcypher.com/ltc/tx/%s"); // LTC
        CurrencyHelper.sTxExplorers.put(Pair.create(Coin.ETH, ""), "https://etherscan.io/tx/%s"); // ETH
        CurrencyHelper.sTxExplorers.put(Pair.create(Coin.XRP, ""), "https://xrpcharts.ripple.com/#/transactions/%s"); // XRP
        CurrencyHelper.sTxExplorers.put(Pair.create(Coin.BCH, ""), "https://explorer.bitcoin.com/bch/tx/%s"); // BCH
        CurrencyHelper.sTxExplorers.put(Pair.create(Coin.EOS, ""), "https://eosflare.io/tx/%s"); // EOS
        CurrencyHelper.sTxExplorers.put(Pair.create(Coin.TRX, ""), "https://tronscan.org/#/transaction/%s"); // TRX
    }
    public static String getBlockExplorerUri(int currency, String tokenAddress, String txid) {
        final String uri = sTxExplorers.get(Pair.create(currency, tokenAddress));
        if (uri != null) {
            return String.format(Locale.getDefault(), uri, txid);
        }
        return null;
    }

    // Coin icon
    public static @DrawableRes
    int getCoinIconResource(Context context, String symbol) {
        return context.getResources().getIdentifier(
                symbol.replace("-", "_").toLowerCase(),
                "drawable", context.getPackageName()
        );
    }

    // find currency by type & token addr
    public static Currency findCurrency(List<Currency> currencies, Wallet wallet) {
        return findCurrency(currencies, wallet.currency, wallet.tokenAddress);
    }

    public static Currency findCurrency(List<Currency> currencies, int currency, String tokenAddress) {
        if (currencies != null) {
            for (Currency c : currencies) { // find currency matches wallet
                if (c.currency == currency &&
                        c.tokenAddress.equals(tokenAddress)) {
                    return c;
                }
            }
        }
        return null;
    }

    public static String getEffectiveBalance(Balance balance) {
        if (balance.tokenBalance.isEmpty()) { // if no token balance, use balance
            return balance.balance;
        }
        return balance.tokenBalance; // or take token balance
    }
}

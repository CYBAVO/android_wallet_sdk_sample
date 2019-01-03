package com.cybavo.example.wallet.detail;

import com.cybavo.wallet.service.wallet.Transaction;

import java.util.ArrayList;
import java.util.List;

public class HistoryList {
    boolean init = false;
    boolean loading = false;
    boolean hasMore = true;
    List<Transaction> history = new ArrayList<>();

    @Override
    public String toString() {
        return "HistoryList{" +
                "init=" + init +
                ", loading=" + loading +
                ", hasMore=" + hasMore +
                ", history=" + history +
                '}';
    }
}

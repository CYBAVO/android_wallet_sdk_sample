package com.cybavo.example.wallet.main;

import com.cybavo.wallet.service.wallet.Balance;

public class BalanceEntry {

    public final Balance balance;
    public final long updatedAt;
    public final boolean init;

    public BalanceEntry(Balance balance, long updatedAt, boolean init) {
        this.balance = balance;
        this.updatedAt = updatedAt;
        this.init = init;
    }

    @Override
    public String toString() {
        return "BalanceEntry{" +
                "balance='" + balance + '\'' +
                ", updatedAt=" + updatedAt +
                ", init=" + init +
                '}';
    }
}

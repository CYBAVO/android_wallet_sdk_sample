/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.main;

import java.util.Objects;

public class BalanceKey {

    public final int currency;
    public final String tokenAddress;
    public final String address;

    public BalanceKey(int currency, String tokenAddress, String address) {
        this.currency = currency;
        this.tokenAddress = tokenAddress;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BalanceKey key = (BalanceKey) o;
        return currency == key.currency &&
                Objects.equals(tokenAddress, key.tokenAddress) &&
                Objects.equals(address, key.address);
    }

    @Override
    public int hashCode() {

        return Objects.hash(currency, tokenAddress, address);
    }
}

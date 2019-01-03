package com.cybavo.example.wallet.detail;

import android.os.Parcel;
import android.os.Parcelable;

import com.cybavo.wallet.service.wallet.Transaction;

public class TransactionParcelable implements Parcelable {

    static TransactionParcelable fromTransaction(Transaction transaction) {
        return new TransactionParcelable(transaction);
    }

    static Transaction toTransaction(Parcelable parcelable) {
        return ((TransactionParcelable) parcelable).transaction;
    }

    final Transaction transaction;

    private TransactionParcelable(Transaction transaction) {
        this.transaction = transaction;
    }

    protected TransactionParcelable(Parcel in) {
        final Transaction tx = new Transaction();
        tx.txid = in.readString();
        tx.fromAddress = in.readString();
        tx.toAddress = in.readString();
        tx.amount = in.readString();
        tx.transactionFee = in.readString();
        tx.timestamp = in.readLong();
        tx.pending = in.readByte() != 0;
        this.transaction = tx;
    }

    public static final Creator<TransactionParcelable> CREATOR = new Creator<TransactionParcelable>() {
        @Override
        public TransactionParcelable createFromParcel(Parcel in) {
            return new TransactionParcelable(in);
        }

        @Override
        public TransactionParcelable[] newArray(int size) {
            return new TransactionParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(transaction.txid);
        dest.writeString(transaction.fromAddress);
        dest.writeString(transaction.toAddress);
        dest.writeString(transaction.amount);
        dest.writeString(transaction.transactionFee);
        dest.writeLong(transaction.timestamp);
        dest.writeByte((byte) (transaction.pending ? 1 : 0));
    }
}

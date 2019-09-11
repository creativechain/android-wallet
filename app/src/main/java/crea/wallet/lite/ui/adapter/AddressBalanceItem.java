package crea.wallet.lite.ui.adapter;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.creativecoinj.core.Address;

import crea.wallet.lite.application.Constants;

public class AddressBalanceItem implements Parcelable {

    private String address;
    private double balance;
    private boolean selected;

    public AddressBalanceItem(String address, double balance) {
        this.address = address;
        this.balance = balance;
    }

    protected AddressBalanceItem(Parcel in) {
        address = in.readString();
        balance = in.readDouble();
        selected = in.readInt() == 1;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public static final Creator<AddressBalanceItem> CREATOR = new Creator<AddressBalanceItem>() {
        @Override
        public AddressBalanceItem createFromParcel(Parcel in) {
            return new AddressBalanceItem(in);
        }

        @Override
        public AddressBalanceItem[] newArray(int size) {
            return new AddressBalanceItem[size];
        }
    };

    public String getAddress() {
        return address;
    }

    public double getBalance() {
        return balance;
    }

    public Address toAddress() {
        return Address.fromBase58(Constants.WALLET.NETWORK_PARAMETERS, address);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeDouble(balance);
        parcel.writeInt(isSelected() ? 1 : 0);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof AddressBalanceItem) {
            AddressBalanceItem aObj = (AddressBalanceItem) obj;
            return aObj.address.equals(this.address);
        }
        return false;

    }
}

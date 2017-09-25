package crea.wallet.lite.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import org.creativecoinj.core.Address;

import java.util.List;

import static crea.wallet.lite.application.Constants.WALLET.NETWORK_PARAMETERS;

/**
 * Created by ander on 17/11/16.
 */
@Table(name = "book_address")
public class BookAddress extends Model {

    private static final String TAG = "BookAddress";

    @Column(name = "label")
    private String label = "";

    @Column(name = "addressee", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private String address;

    @Column(name = "mine")
    private boolean isMine = false;

    public BookAddress() {
        super();
    }

    public String getLabel() {
        return label;
    }

    public BookAddress setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public BookAddress setAddress(String address) {
        this.address = address;
        return this;
    }

    public boolean isMine() {
        return isMine;
    }

    public BookAddress setMine(boolean mine) {
        isMine = mine;
        return this;
    }

    public Address toAddress() {
        return Address.fromBase58(NETWORK_PARAMETERS, address);
    }

    public static List<BookAddress> find(boolean walletAddresses) {
        return new Select().from(BookAddress.class).where("mine = " + (walletAddresses ? 1 : 0)).execute();
    }

    public static List<BookAddress> findAll() {
        return new Select().from(BookAddress.class).execute();
    }

    public static BookAddress resolveLabel(String label) {
        return new Select().from(BookAddress.class).where("label = '" + label + "'").executeSingle();
    }

    public static BookAddress resolveAddress(String address) {
        return new Select().from(BookAddress.class).where("addressee = '" + address + "'").executeSingle();
    }

    public static BookAddress resolveAddress(Address address) {
        return new Select().from(BookAddress.class).where("addressee = '" + address.toString() + "'").executeSingle();
    }

    public static void add(List<Address> mainReceiveAddresses, boolean isFromWallet) {
        for (Address a : mainReceiveAddresses) {
            BookAddress b = resolveAddress(a);
            if (b == null) {
                b = new BookAddress()
                        .setAddress(a.toString())
                        .setMine(isFromWallet);
                b.save();
            }
        }
    }
}

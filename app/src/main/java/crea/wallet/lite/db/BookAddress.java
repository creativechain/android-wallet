package crea.wallet.lite.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import org.creativecoinj.core.Address;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

import static crea.wallet.lite.application.Constants.WALLET.ADDRESS_BOOK_FILE;
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
        return label == null ? "" : label;
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

    public final Long secureSave() {
        Long id = save();
        if (!isMine) {
            saveContacts(ADDRESS_BOOK_FILE);
        }
        return id;
    }

    public final void secureDelete() {
        delete();
        if (!isMine) {
            saveContacts(ADDRESS_BOOK_FILE);
        }
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
                b.secureSave();
            }
        }
    }

    public static void loadContacts(File contactFile) {
        if (contactFile.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(contactFile));
                HashMap<String, String> contactStore = (HashMap<String, String>) ois.readObject();
                ois.close();

                for (String address : contactStore.keySet()) {
                    BookAddress b = resolveAddress(address);
                    if (b == null) {
                        b = new BookAddress()
                                .setAddress(address)
                                .setLabel(contactStore.get(address))
                                .setMine(false);
                        b.secureSave();
                    } else if (b.getLabel().isEmpty()) {
                        b.setLabel(contactStore.get(address));
                        b.secureSave();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveContacts(File contactFile) {
        try {
            List<BookAddress> contacts = find(false);

            HashMap<String, String> contactStore = new HashMap<>();
            for (BookAddress address : contacts) {
                contactStore.put(address.getAddress(), address.getLabel());
            }

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(contactFile));
            oos.writeObject(contactStore);

            oos.flush();
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

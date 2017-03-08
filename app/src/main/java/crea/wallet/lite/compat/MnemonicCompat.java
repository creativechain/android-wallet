package crea.wallet.lite.compat;

import java.util.List;

/**
 * Created by Andersson G. Acosta on 21/02/17.
 */
public interface MnemonicCompat {

    List<String> encode(String hexMnemonic);
    String decode(List<String> wordList);
    String[] getWordList();
    int getWordListLength();

}

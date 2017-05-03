package crea.wallet.lite.util;

import org.creativecoinj.crypto.MnemonicCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ander on 15/03/16.
 */
public class MnemonicWordSearcher {

    private static final String TAG = "MnemonicWordSearcher";

    private List<String> wordMap;

    private static final int ALL_MATCHES = 0;

    public MnemonicWordSearcher() {
        wordMap = MnemonicCode.INSTANCE.getWordList();
    }

    public List<String> search(String startWith, int maxMatches) {
        List<String> matches = new ArrayList<>();

        if (startWith != null  && !startWith.isEmpty()) {
            boolean hasMatches = false;
            for (String s : wordMap) {
                if (s.startsWith(startWith)) {
                    matches.add(s);
                    hasMatches = true;
                } else if (!s.startsWith(startWith) && hasMatches) {
                    break;
                }

                if (matches.size() == maxMatches && maxMatches != ALL_MATCHES) {
                    break;
                }
            }
        }

        return matches;
    }

    public List<String> search(String startWith) {
        return search(startWith, ALL_MATCHES);
    }
}

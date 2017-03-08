package crea.wallet.lite.compat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andersson G. Acosta on 21/02/17.
 */
public class OldElectrumMnemonicCompat implements MnemonicCompat {

    private static final String TAG = "ElectrumMnemonicCompat";

    private static final int N = 1626;

    private List<String> wordList;

    public OldElectrumMnemonicCompat(List<String> wordList) {
        this.wordList = wordList;
    }

    @Override
    public List<String> encode(String passphrase) {
        int length = passphrase.length();
        assert length % 8 == 0;
        ArrayList<String> out =  new ArrayList<>();

        for (int i = 0; i < length / 8; i++) {
            String word = passphrase.substring(8*i, 8*i+8);
            int x = Integer.valueOf(word, 16);
            int w1 = (x%N);
            int w2 = ((x/N) + w1) % N;
            int w3 = ((x/N/N) + w2) % N;

            out.add(wordList.get(w1));
            out.add(wordList.get(w2));
            out.add(wordList.get(w3));
        }
        return out;
    }

    @Override
    public String decode(List<String> list) {
        String out = "";
        int lenght = list.size();

        for (int i = 0; i < lenght / 3; i++) {
            List<String> subList = list.subList(3*i, 3*i+3);
            int w1 = wordList.indexOf(subList.get(0));
            int w2 = wordList.indexOf(subList.get(1))%N;
            int w3 = wordList.indexOf(subList.get(2))%N;

            int x = w1 + N * ((w2 - w1)%N) + N*N*((w3-w2)%N);
            out += String.format("%08x", x);
        }
        return out;
    }

    @Override
    public String[] getWordList() {
        return wordList.toArray(new String[]{});
    }

    @Override
    public int getWordListLength() {
        return wordList.size();
    }
}

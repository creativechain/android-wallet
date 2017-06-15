package crea.wallet.lite.ui.tool;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.creativecoinj.crypto.MnemonicCode;

import crea.wallet.lite.R;
import crea.wallet.lite.util.MnemonicWordSearcher;
import crea.wallet.lite.widget.keyboard.OnButtonPressedListener;
import crea.wallet.lite.widget.keyboard.TextKeyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.kaede.tagview.OnTagClickListener;
import me.kaede.tagview.Tag;
import me.kaede.tagview.TagView;

/**
 * Created by ander on 16/03/16.
 */
public class SeedActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "SeedActivity";

    public static final String EXTRA_SEED = "extra_seed";

    private TagView seed;
    private String seedWord = "";
    private String[] tags;

    private TagView word;
    private TextView search;
    private RadioButton english;
    private RadioButton spanish;
    private RadioButton french;
    private RadioButton italian;
    private MnemonicWordSearcher searcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_seed);

        english = (RadioButton) findViewById(R.id.english);
        spanish = (RadioButton) findViewById(R.id.spanish);
        french = (RadioButton) findViewById(R.id.french);
        italian = (RadioButton) findViewById(R.id.italian);

        english.setOnCheckedChangeListener(this);
        spanish.setOnCheckedChangeListener(this);
        french.setOnCheckedChangeListener(this);
        italian.setOnCheckedChangeListener(this);

        final TextKeyboard keyboard = (TextKeyboard) findViewById(R.id.text_keyboard);
        seed = (TagView) findViewById(R.id.seed);
        word = (TagView) findViewById(R.id.word);
        search = (TextView) findViewById(R.id.search);

        seed.setOnTagClickListener(new OnTagClickListener() {
            @Override
            public void onTagClick(Tag tag, int i) {
                seed.remove(i);
            }
        });

        word.setOnTagClickListener(new OnTagClickListener() {
            @Override
            public void onTagClick(Tag tag, int i) {
                seed.addTag(new Tag(tag.text));
                word.removeAllTags();
                seedWord = "";
                search.setText("");
            }
        });

        keyboard.setOnButtonPressedListener(new OnButtonPressedListener() {
            @Override
            public void onButtonPressed(int key) {
                word.removeAllTags();

                if (key == DELETE_KEY && seedWord.length() >0) {
                    seedWord = seedWord.substring(0, seedWord.length()-1);
                } else if (key == OK_KEY) {
                    if (!seedWord.isEmpty()) {
                        seed.addTag(new Tag(seedWord));
                        seedWord = "";
                    }
                } else if (key >= KEY_A && key <= KEY_Z){
                    seedWord = seedWord + keyboard.letterToString();
                }

                search.setText(seedWord);

                List<String> wordMap = searcher.search(seedWord, 5);

                tags = new String[wordMap.size()];
                tags = wordMap.toArray(tags);
                word.addTags(tags);


            }
        });

        setLanguage();
    }

    private void setLanguage() {
        String lang = Locale.getDefault().getCountry().toLowerCase();
        RadioButton toChecked;
        switch (lang) {
            case "es":
                toChecked = spanish;
                break;
            case "fr":
                toChecked = french;
                break;
            case "it":
                toChecked = italian;
                break;
            default:
                toChecked = english;
                break;
        }

        toChecked.setChecked(true);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            int id = buttonView.getId();
            String locale;
            switch (id) {
                case R.id.spanish:
                    locale = "es";
                    break;
                case R.id.french:
                    locale = "fr";
                    break;
                case R.id.italian:
                    locale = "it";
                    break;
                default:
                    locale = "en";
            }

            loadSearcher(locale);
        }
    }

    private void loadSearcher(String locale) {
        try	{
            String path = "bitcoin/wordlist/" + locale + ".txt";
            MnemonicCode.INSTANCE = new MnemonicCode(getAssets().open(path), null);
            searcher = new MnemonicWordSearcher();
            clearAllTags();
        } catch (final IOException x) {
            throw new Error(x);
        }
    }

    private void clearAllTags() {
        tags = new String[0];
        seed.removeAllTags();
        seedWord = "";
        word.removeAllTags();
        search.setText(seedWord);
    }

    private void cancelResult() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void okResult() {
        Intent data = new Intent();
        ArrayList<String> list = new ArrayList<>();
        List<Tag> t = seed.getTags();
        for (Tag tag : t) {
            list.add(tag.text);
        }

        data.putStringArrayListExtra(EXTRA_SEED, list);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_seed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                cancelResult();
                break;
            case R.id.action_ok:
                okResult();
        }
        return super.onOptionsItemSelected(item);
    }
}

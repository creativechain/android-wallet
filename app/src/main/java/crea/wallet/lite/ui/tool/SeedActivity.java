package crea.wallet.lite.ui.tool;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import crea.wallet.lite.R;
import crea.wallet.lite.util.MnemonicWordSearcher;
import crea.wallet.lite.widget.keyboard.OnButtonPressedListener;
import crea.wallet.lite.widget.keyboard.TextKeyboard;

import java.util.ArrayList;
import java.util.List;

import me.kaede.tagview.OnTagClickListener;
import me.kaede.tagview.Tag;
import me.kaede.tagview.TagView;

/**
 * Created by ander on 16/03/16.
 */
public class SeedActivity extends AppCompatActivity {

    private static final String TAG = "SeedActivity";

    public static final String EXTRA_SEED = "extra_seed";

    private TagView seed;
    private String seedWord = "";
    private String[] tags;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_seed);

        final MnemonicWordSearcher searcher = new MnemonicWordSearcher();

        final TextKeyboard keyboard = (TextKeyboard) findViewById(R.id.text_keyboard);
        seed = (TagView) findViewById(R.id.seed);
        final TagView word = (TagView) findViewById(R.id.word);
        final TextView search = (TextView) findViewById(R.id.search);

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

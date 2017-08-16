package crea.wallet.lite.background;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.zanjou.http.debug.Logger;
import com.zanjou.http.request.Request;
import com.zanjou.http.response.BaseResponseListener;
import com.zanjou.http.response.ResponseListener;

import org.creativecoinj.core.Coin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import crea.wallet.lite.BuildConfig;
import crea.wallet.lite.application.Configuration;
import crea.wallet.lite.application.Constants;
import crea.wallet.lite.wallet.FeeCategory;

import static crea.wallet.lite.application.Constants.URLS.FEES_URL;

/**
 * Created by Andersson G. Acosta on 13/06/17.
 */

public class DynamicFeeLoader extends AsyncTask<Void, Map<FeeCategory, Coin>, Void> {

    private static final String TAG = "DynamicFeeLoader";
    private Context context;

    public DynamicFeeLoader(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            try {
                fetchDynamicFees();
            } catch (Exception e) {
                Map<FeeCategory, Coin> staticFees = parseFees(context.getAssets().open(Constants.FILES.FEES_FILENAME));
                publishProgress(staticFees);
            }
        } catch (IOException e) {
            throw  new RuntimeException(e);
        }
        return null;
    }

    @SafeVarargs
    @Override
    protected final void onProgressUpdate(Map<FeeCategory, Coin>... values) {
        Map<FeeCategory, Coin> staticFees = values[0];
        saveFees(staticFees);
    }

    private void saveFees(Map<FeeCategory, Coin> staticFees) {
        Configuration conf = Configuration.getInstance();
        for (FeeCategory category : staticFees.keySet()) {
            conf.setTransactionFee(category, staticFees.get(category));
        }
    }

    private Map<FeeCategory, Coin> parseFees(final InputStream is) throws IOException {
        final Map<FeeCategory, Coin> dynamicFees = new HashMap<>();
        BufferedReader reader = null;
        String line = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, Charsets.US_ASCII));
            while (true) {
                line = reader.readLine();
                if (line == null)
                    break;
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;

                final String[] fields = line.split("=");
                try {
                    final FeeCategory category = FeeCategory.valueOf(fields[0]);
                    final Coin rate = Coin.valueOf(Long.parseLong(fields[1]));
                    dynamicFees.put(category, rate);
                } catch (IllegalArgumentException x) {
                    Log.w("Cannot parse line, ignoring: '" + line + "'", x);
                }
            }
        } catch (final Exception x) {
            throw new RuntimeException("Error while parsing: '" + line + "'", x);
        } finally {
            if (reader != null)
                reader.close();
            is.close();
        }
        return dynamicFees;
    }

    private void fetchDynamicFees() {
        Request.create(FEES_URL)
                .setLogger(new Logger(Logger.ERROR))
                .setMethod(Request.GET)
                .addHeader("User-Agent", BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME)
                .setResponseListener(new BaseResponseListener() {
                    @Override
                    public void onErrorResponse(int i, String s) {

                    }

                    @Override
                    public void onOkResponse(String s) {
                        try {
                            Map<FeeCategory, Coin> staticFees = parseFees(new ByteArrayInputStream(s.getBytes()));
                            publishProgress(staticFees);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).execute();
    }
}

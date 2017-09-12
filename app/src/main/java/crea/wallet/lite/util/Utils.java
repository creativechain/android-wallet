package crea.wallet.lite.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import crea.wallet.lite.application.Constants;

/**
 * Created by personal on 12/22/14.
 */
public class Utils {
    private static final String TAG = "Utils";
    private static final int LOWERCASE = 0;
    private static final int UPPERCASE = 1;
    private static final int DIGIT = 2;
    private static final int WALLET_ENCRYPTION_TIME = 1;
    private static final int MASTERPASSWORD_ENCRYPTION_TIME = 2;
    private static final int MASTERPASSWORD_FOR_WALLET_ENCRYPTION_TIME = 1;
    private static final String AES = "AES";
    private static final String SHA256 = "SHA-256";
    public static final int SALT_ENCRYPTED_LENGHT = 28;
    public static final int MNEMONIC_BYTE_ARRAY_LENGHT = 16;


    public static List<String> getPrefixes(Activity activity) {

        AssetManager assetMan = activity.getAssets();
        List<String> prefixCountry = new ArrayList<>();
        try {
            InputStream is = assetMan.open("countrylist.xml");
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            org.w3c.dom.Element rootElement = doc.getDocumentElement();
            NodeList countries = rootElement.getElementsByTagName("row");
//            Log.d(TAG, "Total countries: " + countries.getLength());

            for (int i = 0; i < countries.getLength(); i++) {
                org.w3c.dom.Element countryElement = (org.w3c.dom.Element) countries.item(i);
                if (countryElement.getElementsByTagName("Col9").item(0).hasChildNodes()) {
                    String prefix = countryElement.getElementsByTagName("Col9").item(0).getFirstChild().getNodeValue();
                    String country = countryElement.getElementsByTagName("Col1").item(0).getFirstChild().getNodeValue();
                    prefixCountry.add(prefix + " " + country);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return prefixCountry;
    }

    public static int convertDpToPixel(float dp, Context context){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int convertPixelsToDp(float px, Context context){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int getRandom(final int MINOR, final int MAYOR) {
        return (int) Math.floor(Math.random()*(MAYOR-MINOR+1)+MINOR);
    }

    public static long getRandom(final long MINOR, final long MAYOR) {
        return Math.round(Math.floor(Math.random()*(MAYOR-MINOR+1)+MINOR));
    }

    public static String generateRandomString(int length) {
        String id = "";

        for (int x = 0; x < length; x++) {
            int method = getRandom(0, 2);
            char c = 0;

            switch (method) {
                case LOWERCASE:
                    c = (char) getRandom(97, 122);
                    break;
                case UPPERCASE:
                    c = (char) getRandom(65, 90);
                    break;
                case DIGIT:
                    c = (char) getRandom(48, 57);
                    break;
            }

            id = id + String.valueOf(c);
        }

        return id;
    }

    public static String generateStringWithByteLength(int length) {
        String id = "";

        while (id.getBytes().length < length) {
            int method = getRandom(0, 2);
            char c = 0;
            switch (method) {
                case LOWERCASE:
                    c = (char) getRandom(97, 122);
                    break;
                case UPPERCASE:
                    c = (char) getRandom(65, 90);
                    break;
                case DIGIT:
                    c = (char) getRandom(48, 57);
                    break;
            }

            id = id + String.valueOf(c);
        }

        Log.i(TAG, "id bytes length: " + id.getBytes().length);
        Log.i(TAG, "id: " + id);

        return id;
    }

    public static long[] getLongValues(JSONArray jsonArray) throws JSONException {
        int length = jsonArray.length();
        long[] values;
        if (length == 0) {
            values = new long[1];
            values[0] = 0;
            return values;
        }
        values = new long[length];

        for (int x = 0; x < length; x++) {
            values[x] = jsonArray.getInt(x);
        }

        return values;
    }

    public static List<String> getSEPACountries(Activity activity) {

        AssetManager assetMan = activity.getAssets();
        List<String> prefixCountry = new ArrayList<>();
        try {
            InputStream is = assetMan.open("sepacountrieslist.xml");
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            org.w3c.dom.Element rootElement = doc.getDocumentElement();
            NodeList countries = rootElement.getElementsByTagName("row");
//            Log.d(TAG, "Total countries: " + countries.getLength());

            for (int i = 0; i < countries.getLength(); i++) {
                org.w3c.dom.Element countryElement = (org.w3c.dom.Element) countries.item(i);
                if (countryElement.getElementsByTagName("Col10").item(0).hasChildNodes()) {
                    String countryCode2Letter = countryElement.getElementsByTagName("Col10").item(0).getFirstChild().getNodeValue();
                    String country = countryElement.getElementsByTagName("Col1").item(0).getFirstChild().getNodeValue();
                    prefixCountry.add(countryCode2Letter + " " + country);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return prefixCountry;
    }

    public static long toNanoCoins(String value) {
        double val = Double.parseDouble(value) * 1e8d;
        return BigDecimal.valueOf(val).longValue();
    }

    public static String[] getCountryNamesByISOCode(String... isoCodes) {
        String[] countries = new String[isoCodes.length];
        for (int x = 0; x < isoCodes.length; x++) {
            countries[x] = getCountryNameByISOCode(isoCodes[x]);
        }

        return countries;
    }

    public static String getCountryNameByISOCode(String isoCode) {
        isoCode = isoCode.toUpperCase();
        if (isoCode.equals("EZ")) {
            return  "Eurozone";
        }

        return new Locale("", isoCode).getDisplayCountry();
    }

    public static String findByRegularExpression(CharSequence input, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(input);
        boolean found = matcher.find();
        if (found) {
            return matcher.group();
        }

        return null;
    }

    public static boolean stringContainsText(String text) {
        return text != null && !text.isEmpty();
    }

    public static String encryptInSHA2(String masterpassword, int times) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] temp = masterpassword.getBytes();
        for (int x = 0; x < times; x++) {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            temp = digest.digest(temp);
        }
        return new String(temp);
    }

    public static String generateSalt() {
        String salt = generateStringWithByteLength(SALT_ENCRYPTED_LENGHT);
        Log.e(TAG, "SALT bytes lenght: " + salt.getBytes().length);
        return salt;
    }


    public static String deriveMasterpassword(String masterpassword) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return encryptInSHA2(masterpassword, MASTERPASSWORD_ENCRYPTION_TIME);
    }

    public static String encrypt(String key, String toEncrypt) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        char[] password = key.toCharArray();
        byte[] salt = key.getBytes();
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(password, salt, 8192, 256);
        SecretKey tmp = kf.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] stringBytes = toEncrypt.getBytes();
        byte[] encrypted = cipher.doFinal(stringBytes);
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    public static byte[] getRandomByteArray(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }

    public static String decrypt(String key, String encodedString) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException {

        char[] password = key.toCharArray();
        byte[] salt = key.getBytes();
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(password, salt, 8192, 256);
        SecretKey tmp = kf.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] encryptedBytes = Base64.decode(encodedString, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(encryptedBytes);
        return new String(decrypted);
    }

    public static String capitalize(String str) {

        if (TextUtils.isEmpty(str)) {
            return str;
        }

        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase += Character.toUpperCase(c);
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase += c;
        }
        return phrase;
    }

    public static Bitmap textAsBitmap(String mText, float textSize, int textColor, int width, int height){

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        TextPaint mTextPaint=new TextPaint();
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(textColor);
        StaticLayout mTextLayout = new StaticLayout(mText, mTextPaint, canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.save();
        mTextLayout.draw(canvas);
        canvas.restore();
        return image;
    }

    public static <T> T[] listToArray(List<T> list) {
        return (T[]) list.toArray();
    }

    public static long normalizeWalletTime(long millis) {
        long genesisTime = Constants.WALLET.MIN_CREATION_TIME * 1000;
        if (millis < genesisTime) {
            return genesisTime;
        }

        return millis;
    }
}

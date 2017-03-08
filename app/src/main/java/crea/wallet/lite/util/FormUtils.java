package crea.wallet.lite.util;

import android.widget.EditText;

/**
 * Created by personal on 1/12/15.
 */
public class FormUtils {

    public static boolean email(EditText editText){
        String toValidate = editText.getText().toString();
        if (!isEmpty(editText)) {
            int indexArroba = toValidate.indexOf("@");
            if (indexArroba > 0) {
                int indexDot = toValidate.indexOf(".", indexArroba);
                return indexDot > (indexArroba+1);
            }
        }

        return false;
    }

    public static boolean equals(EditText editText, EditText editText2){
        String toValidate = editText.getText().toString();
        String toValidate2 = editText2.getText().toString();
        return toValidate.equals(toValidate2);
    }

    public static boolean isEmpty(EditText... editTexts){
        for (EditText editText : editTexts) {
            if (editText.getText().toString().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkValues(EditText editText, String... values) {
        String editVal = editText.getText().toString();
        for (String val : values) {
            if (editVal.equals(val)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkValues(EditText editText, double... values) {
        String[] doubleVals = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            doubleVals[x] = String.valueOf(values[x]);
        }
        return checkValues(editText, doubleVals);
    }

    public static boolean checkValues(EditText editText, long... values) {
        String[] longValues = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            longValues[x] = String.valueOf(values[x]);
        }
        return checkValues(editText, longValues);
    }

    public static boolean containsDecimal(EditText editText) {
        if (isEmpty(editText)) {
            return false;
        }

        try {
            double d = Double.parseDouble(editText.getText().toString());
            return d > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean containsInteger(EditText editText) {
        try {
            long l = Long.parseLong(editText.getText().toString());
            return l > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}

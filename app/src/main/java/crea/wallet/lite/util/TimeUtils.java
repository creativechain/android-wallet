package crea.wallet.lite.util;

import crea.wallet.lite.R;
import crea.wallet.lite.application.WalletApplication;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by ander on 11/05/16.
 */
public class TimeUtils {

    private static final String TAG = "TimeString";

    public static final long SECOND = 1000L;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = 60* MINUTE;
    public static final long DAY = 24 * HOUR;
    public static final long YESTERDAY = 2 * DAY;
    public static final long BEFORE_YESTERDAY = 3 * DAY;
    public static final long WEEK = 7 * DAY;
    public static final long MONTH = 30 * DAY;
    public static final long YEAR = 365 * DAY;

    public static boolean isLastDayOfMonth(Calendar now) {
        return now.get(Calendar.DAY_OF_MONTH) == now.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static boolean isLastMonthOfYear(Calendar now) {
        return now.get(Calendar.MONTH) == now.getActualMaximum(Calendar.MONTH);
    }

    public static long getDayTime() {
        Calendar c = Calendar.getInstance();
        Calendar now = new GregorianCalendar(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        return now.getTimeInMillis();
    }

    public static long getTomorrowTime() {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);
        if (isLastDayOfMonth(c)) {
            day = 1;
            month++;
            if (isLastMonthOfYear(c)) {
                month = 1;
                year++;
            }
        } else {
            day += 1;
        }
        Calendar now = new GregorianCalendar(year, month, day);
        return now.getTimeInMillis();
    }

    public static long getMonthTime() {
        Calendar c = Calendar.getInstance();
        Calendar now = new GregorianCalendar(c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1);
        return now.getTimeInMillis();
    }

    public static long getNextMonthTime() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        if (isLastMonthOfYear(c)) {
            month = 1;
            year++;
        } else {
            month += 1;
        }
        Calendar now = new GregorianCalendar(year, month, 1);
        return now.getTimeInMillis();
    }

    public static long getYearTime() {
        Calendar c = Calendar.getInstance();
        Calendar now = new GregorianCalendar(c.get(Calendar.YEAR), 1, 1);
        return now.getTimeInMillis();
    }

    public static long getNextYearTime() {
        Calendar c = Calendar.getInstance();
        Calendar now = new GregorianCalendar(c.get(Calendar.YEAR)+1, 1, 1);
        return now.getTimeInMillis();
    }

    private static String addZero(int num) {
        return "" + (num < 10 ? "0" + num : num);
    }

    private static String getFormattedHour(Calendar c) {
        return c.get(Calendar.HOUR_OF_DAY) + ":" + addZero(c.get(Calendar.MINUTE));
    }

    private static String getFormattedDay(Calendar c) {
        return WalletApplication.INSTANCE.getString(R.string.day_at,
                c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()),
                c.get(Calendar.DAY_OF_MONTH),
                getFormattedHour(c));
    }

    private static String getFormattedMonth(Calendar c) {
        return WalletApplication.INSTANCE.getString(R.string.month_at,
                c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()),
                c.get(Calendar.DAY_OF_MONTH),
                c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                getFormattedHour(c));
    }

    private static String getFormattedYear(Calendar c) {
        String d = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
        int dN = c.get(Calendar.DAY_OF_MONTH);
        String m = c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        int y = c.get(Calendar.YEAR);
        String h = getFormattedHour(c);
        return WalletApplication.INSTANCE.getString(R.string.year_at, d, dN, m, y, h);
    }

    public static String getTimeString(long date) {
        Calendar c = new GregorianCalendar();
        c.setTime(new Date(date));

        long thisDay = getDayTime();
        long yesterday = thisDay - DAY;
        long beforeYesterday = yesterday - DAY;
        long thisMonth = getMonthTime();
        long thisYear = getYearTime();
        long tomorrow = getTomorrowTime();
        long afterTomorrow = tomorrow + DAY;
        long nextMonth = getNextMonthTime();
        long nextYear = getNextYearTime();

        if (date >= nextYear) { //NEXT YEAR
            return getFormattedYear(c);
        } else if (date >= nextMonth) { //NEXT MONTH
            return getFormattedMonth(c);
        } else if (date >= afterTomorrow) { //AFTER TOMORROW
            return getFormattedMonth(c);
        } else if (date >= tomorrow) { //TOMORROW
            return WalletApplication.INSTANCE.getString(R.string.tomorrow_at, getFormattedHour(c));
        } else if (date > thisDay) { //TODAY
            return WalletApplication.INSTANCE.getString(R.string.today_at, getFormattedHour(c));
        } else if (date < thisDay && date > (thisDay - DAY)) { //YESTERDAY
            return WalletApplication.INSTANCE.getString(R.string.yesterday_at, getFormattedHour(c));
        } else if (date < (thisDay - DAY) && date > (thisDay - WEEK) && date > thisMonth) { //BEFORE YESTERDAY
            return getFormattedDay(c);
        } else if (date < (thisDay - WEEK) && date > thisMonth) { //LAST WEEK
            return getFormattedDay(c);
        } else if (date < thisMonth && date > thisYear) { //LAST MONTH
            return getFormattedMonth(c);
        } else { //LAST YEAR
            return getFormattedYear(c);
        }
    }

    public static String toHumanReadable(long millis)  {
        return  String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }
}

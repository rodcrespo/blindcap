package com.eys.blindcap;

/**
 * Created by nestorrubiogarcia on 08/04/2016.
 *
 * This class makes easy to extract time data from a value in milliseconds.
 */
public class TimeData {

    private long millis;
    private long tens;
    private long secs;
    private long mins;
    private long hrs;

    private String tensString;
    private String secsString;
    private String minsString;
    private String hrsString;

    private String string;

    final private String[] digits = new String[8];
    static final int HOURS_TENS = 0;
    static final int HOURS_ONES = 1;
    static final int MINUTES_TENS = 2;
    static final int MINUTES_ONES = 3;
    static final int SECONDS_TENS = 4;
    static final int SECONDS_ONES = 5;
    static final int TENS_TENS = 6;
    static final int TENS_ONES = 7;


    public TimeData() {
        set(0);
    }

    public TimeData(long millis) {
        set(millis);
    }

    public TimeData(TimeData t) {
        set(t.getMillis());
    }

    public long getMillis() {
        return millis;
    }

    public long getTens() {
        return tens;
    }

    public long getSecs() {
        return secs;
    }

    public long getMins() {
        return mins;
    }

    public long getHrs() {
        return hrs;
    }

    public String getDigit(int index) {
        return digits[index];
    }

    public String getString() {
        return string;
    }

    public void set(long millis) {
        this.millis = millis;

        secs = (millis / 1000) % 60;
        tens = ((millis - secs * 1000) / 10) % 100;
        mins = ((millis / 1000) / 60) % 60;
        hrs =  (((millis / 1000) / 60) / 60) % 24;

        secsString = String.valueOf(secs);
        secsString = (secs < 10 && secs >= 0) ? "0" + secsString : secsString;

        minsString = String.valueOf(mins);
        minsString = (mins < 10 && mins >= 0) ? "0" + minsString : minsString;

        hrsString = String.valueOf(hrs);
        hrsString = (hrs < 10 && hrs >= 0) ? "0" + hrsString : hrsString;

        tensString = String.valueOf(tens);
        tensString = (tens < 10 && tens >= 0) ? "0" + tensString : tensString;

        // update string
        string = minsString + ":" + secsString + ":" + tensString;

        // update digits array
        digits[0] = hrsString.substring(0,1);   // tens
        digits[1] = hrsString.substring(1,2);   // ones
        digits[2] = minsString.substring(0,1);
        digits[3] = minsString.substring(1,2);
        digits[4] = secsString.substring(0,1);
        digits[5] = secsString.substring(1,2);
        digits[6] = tensString.substring(0,1);
        digits[7] = tensString.substring(1,2);
    }
}

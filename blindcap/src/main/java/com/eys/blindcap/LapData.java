package com.eys.blindcap;

/**
 * Created by nestorrubiogarcia on 10/04/2016.
 */
public class LapData implements Comparable<LapData> {

    private int index;
    private long millis;


    public LapData(int index, long lapMillis) {
        this.index = index;
        this.millis = lapMillis;
    }


    public LapData(LapData lapData) {
        this.index = lapData.getIndex();
        this.millis = lapData.getMillis();
    }


    public int getIndex() {
        return index;
    }


    public long getMillis() {
        return millis;
    }

    @Override
    public int compareTo(LapData other) {
        // ascending order
        //return (int) (this.millis - other.getMillis());

        // descending order do like this
        return (int) (other.getMillis() - this.millis);
    }

    @Override
    public String toString() {
        return "[ index=" + index + ", millis=" + millis + "]";
    }

}

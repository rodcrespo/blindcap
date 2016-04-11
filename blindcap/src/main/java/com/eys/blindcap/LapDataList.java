package com.eys.blindcap;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by nestorrubiogarcia on 10/04/2016.
 *
 * Stores parcelable list of time snapshots in milliseconds.
 */
public class LapDataList extends ArrayList<LapData> implements Parcelable {

    public LapDataList() {

    }


    public LapDataList(Parcel in) {
        readFromParcel(in);
    }


    private void readFromParcel(Parcel in) {
        clear();

        int listSize = in.readInt();

        for (int i = 0; i < listSize; i++) {
            int index = in.readInt();
            long millis = in.readLong();

            LapData item = new LapData(index, millis);

            add(item);
        }
    }


    @Override
    public void writeToParcel(Parcel out, int flags) {
        int listSize = size();

        // write list size
        out.writeInt(listSize);

        // write items
        for (int i = 0; i < listSize; i++) {
            LapData item = get(i);

            int index = item.getIndex();
            long millis = item.getMillis();

            out.writeInt(index);
            out.writeLong(millis);
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }


    @SuppressWarnings("unchecked")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public LapDataList createFromParcel(Parcel in) {
            return new LapDataList(in);
        }
        public Object[] newArray(int arg0) {
            return null;
        }
    };

}

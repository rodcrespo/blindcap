package com.eys.blindcap;

import java.util.ArrayList;

/**
 * Created by nestorrubiogarcia on 07/04/2016.
 */
public class Timer {

    private long startTime;
    private long elapsedTime;
    private boolean isRunning;

    final private TimeData time = new TimeData();

    private ArrayList<TimeData> timeSnapshots = new ArrayList<TimeData>();


    public Timer() {
        reset();
    }

    public void reset() {
        isRunning = false;

        time.set(0);
    }

    public void start() {


        isRunning = true;
    }

    public void stop() {
        isRunning = false;
    }

    public void tick() {
        if (!isRunning) return;

        elapsedTime = System.currentTimeMillis() - startTime;

        time.set(elapsedTime);
    }

    public void takeSnapshot() {
        timeSnapshots.add(new TimeData(time));
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public TimeData getTime() {
        return time;
    }

    public boolean isRunning() {
        return isRunning;
    }
}

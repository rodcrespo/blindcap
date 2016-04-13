package com.eys.blindcap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by nestorrubiogarcia on 07/04/2016.
 */
public class StatsActivity extends Activity {

    private LapDataList lapsData;
    private long totalTime;

    Typeface fontRegular, fontMedium;

    private Animation animScale;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // get the data
        Bundle bundle = getIntent().getExtras();
        lapsData = bundle.getParcelable("LAP_DATA_LIST");
        totalTime = bundle.getLong("TOTAL_TIME");

        // font
        fontRegular = Typeface.createFromAsset(getAssets(), "font/SamsungSharpSans-Regular.ttf");
        fontMedium = Typeface.createFromAsset(getAssets(), "font/SamsungSharpSans-Medium.ttf");

        animScale = AnimationUtils.loadAnimation(this, R.anim.scale_anim);

        // init views
        initGraph();
        initBestLaps();
        initLaps();
        initCloseBtn();
    }


    private void initGraph() {
        // canvas
        CanvasView graphView = (CanvasView) findViewById(R.id.graphView);
        graphView.setData(lapsData);

        // total time
        TextView totalTimeView = (TextView) findViewById(R.id.totalTime);
        String totalTimeString = "TOTAL: " + new TimeData(totalTime).getString();

        totalTimeView.setTypeface(fontMedium);

        totalTimeView.setText(totalTimeString);
    }


    private void initBestLaps() {
        // sort lap data:
        ArrayList<LapData> sortedLapData = new ArrayList<LapData>();
        for (LapData lapData : lapsData) {
            sortedLapData.add(new LapData(lapData));
        }
        Collections.sort(sortedLapData, new Comparator<LapData>() {
            @Override
            public int compare(LapData a, LapData b) {
                return (int) (a.getMillis() - b.getMillis());
            }
        });

        // add items:
        ViewGroup container = (ViewGroup) findViewById(R.id.bestLapsContainer);

        //int numItems = Math.min(sortedLapData.size(), 3);
        int numLaps = sortedLapData.size();
        int numItems = 3;

        for (int i = 0; i < numItems; i++) {
            String lapIndexText;
            String lapTimeText;
            long lapMillis;

            if (i < numLaps) {
                LapData lapData = sortedLapData.get(i);
                int lapIndex = lapData.getIndex();
                lapMillis = lapData.getMillis();

                lapIndexText = "L" + lapIndex;
                lapTimeText = "TIME " + new TimeData(lapMillis).getString();
            } else {
                lapIndexText = "--";
                lapTimeText = "TIME -- -- --";
                lapMillis = 0;
            }

            // set text
            ViewGroup item = (ViewGroup) getLayoutInflater().inflate(R.layout.item_best_lap, container, false);
            ((TextView) item.findViewById(R.id.lapNumber)).setText(lapIndexText);
            ((TextView) item.findViewById(R.id.lapTime)).setText(lapTimeText);

            // set bar width
            // percentage calculated over the slowest lap time
            float slowestLapTime = sortedLapData.get(numLaps-1).getMillis();
            float pct = ((float)(lapMillis)) / slowestLapTime;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    pct);

            item.findViewById(R.id.progressBar_bar).setLayoutParams(params);

            container.addView(item);
        }

        setFonts(container, fontRegular);
    }


    private void initLaps() {
        ViewGroup container = (ViewGroup) findViewById(R.id.lapsContainer);
        ViewGroup leftColumn = (ViewGroup) container.getChildAt(0);
        ViewGroup rightColumn = (ViewGroup) container.getChildAt(1);

        int numLaps = lapsData.size();
        int numItems = 8;

        for (int i = 0; i < numItems; i++) {
            String lapIndexText;
            String lapTimeText;

            if (i < numLaps) {
                LapData lapData = lapsData.get(i);
                int lapIndex = lapData.getIndex();
                long lapMillis = lapData.getMillis();

                lapIndexText = "L" + lapIndex;
                lapTimeText = new TimeData(lapMillis).getString();
            } else {
                lapIndexText = "L" + (i+1);
                lapTimeText = "-- -- --";
            }

            ViewGroup item = (ViewGroup) getLayoutInflater().inflate(R.layout.item_lap, container, false);
            ((TextView) item.findViewById(R.id.lapNumber)).setText(lapIndexText);
            ((TextView) item.findViewById(R.id.lapTime)).setText(lapTimeText);

            if (i % 2 == 0) {
                // align to right of container
//                LinearLayout.LayoutParams params =
//                        new LinearLayout.LayoutParams(
//                                LinearLayout.LayoutParams.WRAP_CONTENT,
//                                LinearLayout.LayoutParams.WRAP_CONTENT
//                        );
//                params.gravity = Gravity.RIGHT;
//                item.setLayoutParams(params);

                leftColumn.addView(item);
            } else {
                rightColumn.addView(item);
            }
        }

        setFonts(container, fontRegular);
    }


    private void closeStats() {
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
        finish();
    }


    private void initCloseBtn() {
        View closeBtn = findViewById(R.id.btnClose);
        closeBtn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    v.startAnimation(animScale);
                    closeStats();
                    return true;
                }
                return false;
            }
        });
    }


    private void setFonts(ViewGroup group, Typeface font) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ViewGroup) {
                setFonts((ViewGroup) child, font);
            }
            else if (child instanceof TextView) {
                ((TextView) child).setTypeface(font);
            }
        }
    }
}

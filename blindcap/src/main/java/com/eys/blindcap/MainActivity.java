package com.eys.blindcap;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnTouchListener {

    // views
    //private TextView timerText;
    private LinearLayout digits;
    private Button startButton;
    private Button turnButton;

    // animation
    private Animation animScale;
    private AnimatorSet animOpacity;

    // timer
    private final int REFRESH_RATE = 10;

    private Handler handler = new Handler();
    private long startTime;
    private long elapsedTime;
    private String hrsString, minsString, secsString, tensString;
    private long tens, secs, mins, hrs;
    private boolean isRunning;

    private Toast toast; // debug only

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //timerText = (TextView) findViewById(R.id.timerText);
        startButton = (Button) findViewById(R.id.startButton);
        turnButton = (Button) findViewById(R.id.turnButton);
        digits = (LinearLayout) findViewById(R.id.timerDigits);

        animScale = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
        animOpacity = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.opacity_anim);
        animOpacity.setTarget(turnButton);

        setTypefaces();
        initStopWatch();
        initTurnButton();
    }

    private void setTypefaces() {
        Typeface regular = Typeface.createFromAsset(getAssets(), "font/SamsungSharpSans-Regular.ttf");
        //Typeface medium = Typeface.createFromAsset(getAssets(), "font/SamsungSharpSans-Medium.ttf");
        //timerText.setTypeface(regular);
        startButton.setTypeface(regular);

        for (int i=0; i<digits.getChildCount(); i++) {
            ((TextView) digits.getChildAt(i)).setTypeface(regular);
        }
    }

    private void initStopWatch() {
        startButton.setOnTouchListener(this);

        //timerText.setText("00:00:00");

        isRunning = false;
    }

    private void initTurnButton() {
        turnButton.setOnTouchListener(this);

        //TODO: setup circles animation
    }

    private void startTimer() {
        startTime = System.currentTimeMillis(); // - elapsedTime;

        handler.removeCallbacks(timerRunable);
        handler.postDelayed(timerRunable, 0);

        isRunning = true;
    }

    private void resetTimer() {
        handler.removeCallbacks(timerRunable);

        //timerText.setText("00:00:00");

        isRunning = false;
    }

    private void updateTimer (float millis){
        secs = (long)(millis / 1000) % 60;
        tens = (long)((millis - secs * 1000) / 10) % 100;
        mins = (long)((millis / 1000) / 60) % 60;
        hrs =  (long)(((millis / 1000) / 60) / 60) % 24;

        secsString = String.valueOf(secs);
        secsString = (secs < 10 && secs >= 0) ? "0" + secsString : secsString;

        minsString = String.valueOf(mins);
        minsString = (mins < 10 && mins >= 0) ? "0" + minsString : minsString;

        hrsString = String.valueOf(hrs);
        hrsString = (hrs < 10 && hrs >= 0) ? "0" + hrsString : hrsString;

        tensString = String.valueOf(tens);
        tensString = (tens < 10 && tens >= 0) ? "0" + tensString : tensString;

        //timerText.setText(minsString + ":" + secsString + ":" + tensString);

        digits = (LinearLayout) findViewById(R.id.timerDigits);
        ((TextView) digits.getChildAt(0)).setText(minsString.substring(0,1));
        ((TextView) digits.getChildAt(1)).setText(minsString.substring(1,2));
        ((TextView) digits.getChildAt(3)).setText(secsString.substring(0,1));
        ((TextView) digits.getChildAt(4)).setText(secsString.substring(1,2));
        ((TextView) digits.getChildAt(6)).setText(tensString.substring(0,1));
        ((TextView) digits.getChildAt(7)).setText(tensString.substring(1,2));

    }

    private Runnable timerRunable = new Runnable() {
        public void run() {
            elapsedTime = System.currentTimeMillis() - startTime;
            updateTimer(elapsedTime);
            handler.postDelayed(this, REFRESH_RATE);
        }
    };

    /*
     * listeners
     */

    private void onTurnClick() {
        //turnButton.startAnimation(animOpacity);
        animOpacity.start();

        // TODO: send bluetooth signal
        // TODO: store current elapsed time

        // debug
        if (toast != null) toast.cancel();
        toast = Toast.makeText(this, "Turn!", Toast.LENGTH_SHORT);
        toast.show();
    }

    private void onTimerClick() {
        startButton.startAnimation(animScale);
        if (isRunning) {
            startButton.setText(R.string.start);
            resetTimer();
        } else {
            startButton.setText(R.string.stop);
            startTimer();
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (v.getId()) {
                case R.id.startButton:
                    onTimerClick();
                    break;
                case R.id.turnButton:
                    onTurnClick();
                    break;
            }
        }
        return true;
    }
}

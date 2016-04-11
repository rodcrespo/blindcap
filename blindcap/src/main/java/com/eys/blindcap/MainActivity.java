package com.eys.blindcap;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity implements View.OnTouchListener {

    private final static int MAX_NUM_SNAPSHOOTS = 8;

    // views
    private RelativeLayout content;
    private LinearLayout digits;
    private Button startButton;
    private Button turnButton;

    private LinearLayout menu;
    private TextView statsButton;
    private TextView helpButton;

    private boolean menuIsOpen;

    // animation
    private Animation animScale;
    private AnimatorSet animOpacity;

    // timer
    private final int TIMER_REFRESH_RATE = 10;
    private Handler handler = new Handler();
    private long startTime;
    private boolean isTimerRunning;

    // time data
    final private TimeData time = new TimeData();
    private ArrayList<Long> timeSnapshots = new ArrayList<Long>();
    private LapDataList lapDataList = new LapDataList();

    // debug only
    private Toast toast;
    private ImageButton buttonMenu;

    Typeface fontRegular;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        content = (RelativeLayout) findViewById(R.id.content);
        startButton = (Button) findViewById(R.id.startButton);
        digits = (LinearLayout) findViewById(R.id.timerDigits);
        turnButton = (Button) findViewById(R.id.turnButton);
        buttonMenu = (ImageButton) findViewById(R.id.buttonMenu);
        menu = (LinearLayout) findViewById(R.id.menu);
        statsButton = (TextView) findViewById(R.id.buttonStats);
        helpButton = (TextView) findViewById(R.id.buttonHelp);

        animScale = AnimationUtils.loadAnimation(this, R.anim.scale_anim); // TODO: use animator

        animOpacity = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.opacity_anim);

        fontRegular = Typeface.createFromAsset(getAssets(), "font/SamsungSharpSans-Regular.ttf");
    }


    @Override
    protected void onStart() {
        super.onStart();

        //clearData();

        setFont();
        initStopWatch();
        initTurnButton();
        initMenu();
    }


    private void clearData() {
        timeSnapshots.clear();
        lapDataList.clear();
    }


    private void setFont() {
        startButton.setTypeface(fontRegular);

        for (int i = 0; i < digits.getChildCount(); i++) {
            TextView digit = (TextView) digits.getChildAt(i);
            digit.setTypeface(fontRegular);
        }

        statsButton.setTypeface(fontRegular);
        helpButton.setTypeface(fontRegular);
    }


    private void initStopWatch() {
        startButton.setOnTouchListener(this);
    }


    private void initTurnButton() {
        turnButton.setOnTouchListener(this);

        //TODO: setup circles animation
    }


    private void initMenu() {
        menu.setVisibility(View.GONE);
        menu.setAlpha(0f);
        content.setVisibility(View.VISIBLE);
        content.setAlpha(1f);

        menuIsOpen = false;

        buttonMenu.setOnTouchListener(this);
        statsButton.setOnTouchListener(this);
        helpButton.setOnTouchListener(this);
    }


    private void openMenu() {
        crossfade(content, menu);

        stopTimer();
        startButton.setText(R.string.start);
    }


    private void closeMenu() {
        crossfade(menu, content);
    }


    private void crossfade(final View viewOut, View viewIn) {
        viewIn.setAlpha(0f);
        viewIn.setVisibility(View.VISIBLE);
        viewIn.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);

        viewOut.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewOut.setVisibility(View.GONE);
                    }
                });
    }


    private void startTimer() {
        startTime = System.currentTimeMillis();

        isTimerRunning = true;

        handler.removeCallbacks(timerRunable);
        handler.postDelayed(timerRunable, 0);

        // TODO: this should be explicitly performed by tapping a "reset" button (currently not present)
        clearData();
    }


    private void resetTimer() {
        time.set(0);
        updateTimerView();
    }


    private void stopTimer() {
        takeTimeSnapshot();

        handler.removeCallbacks(timerRunable);

        isTimerRunning = false;
    }


    private void updateTimer() {
        long elapsedTime = System.currentTimeMillis() - startTime;

        time.set(elapsedTime);
        updateTimerView();
    }


    private void updateTimerView(){
        ((TextView) digits.getChildAt(0)).setText(time.getDigit(TimeData.MINUTES_TENS));
        ((TextView) digits.getChildAt(1)).setText(time.getDigit(TimeData.MINUTES_ONES));
        ((TextView) digits.getChildAt(3)).setText(time.getDigit(TimeData.SECONDS_TENS));
        ((TextView) digits.getChildAt(4)).setText(time.getDigit(TimeData.SECONDS_ONES));
        ((TextView) digits.getChildAt(6)).setText(time.getDigit(TimeData.TENS_TENS));
        ((TextView) digits.getChildAt(7)).setText(time.getDigit(TimeData.TENS_ONES));
    }


    private Runnable timerRunable = new Runnable() {
        public void run() {
            updateTimer();
            handler.postDelayed(this, TIMER_REFRESH_RATE);
        }
    };


    private void takeTimeSnapshot() {
        if (!isTimerRunning) {
            return;
        }

        Log.d("KXS", "SNAPSHOT!");

        int numSnapshots = timeSnapshots.size();

        if (numSnapshots == MAX_NUM_SNAPSHOOTS) {
            timeSnapshots.remove(0);
        }

        // store values
        timeSnapshots.add(time.getMillis());
    }


    private void refreshLapData() {
        int numSnapshots = timeSnapshots.size();

        lapDataList.clear();

        for (int i = 0; i < numSnapshots; i++) {
            long prevSnapshot = (i == 0) ? 0 : timeSnapshots.get(i-1);
            long currSnapshot = timeSnapshots.get(i);
            long lapTime = currSnapshot - prevSnapshot;

            lapDataList.add(new LapData(i+1, lapTime));
        }
    }


    public void goToStats() {
        refreshLapData();

        Bundle bundle = new Bundle();
        bundle.putParcelable("LAP_DATA_LIST", lapDataList); // time data of each lap
        bundle.putLong("TOTAL_TIME", time.getMillis());     // total elapsed time

        Intent intent = new Intent(this, StatsActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);
    }


    //***************
    // Listeners
    //***************


    private void onTurnClick() {
        animOpacity.setTarget(turnButton);
        animOpacity.start();

        // TODO: send bluetooth signal

        takeTimeSnapshot();

        // debug
//        if (toast != null) toast.cancel();
//        toast = Toast.makeText(this, "Turn!", Toast.LENGTH_SHORT);
//        toast.show();
    }


    private void onTimerClick() {
        startButton.startAnimation(animScale);
        if (isTimerRunning) {
            startButton.setText(R.string.start);
            stopTimer();
            //resetTimer();
        } else {
            startButton.setText(R.string.stop);
            startTimer();
        }
    }


    private void onStatsClick() {
        animScale.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animScale.setAnimationListener(null); // remove the listener
                goToStats();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        statsButton.startAnimation(animScale);
    }


    private void onHelpClick() {
        helpButton.startAnimation(animScale);
    }


    private void onMenuClick() {
        if (menuIsOpen) {
            closeMenu();
            menuIsOpen = false;
        } else {
            openMenu();
            menuIsOpen = true;
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
                case R.id.buttonMenu:
                    onMenuClick();
                    break;
                case R.id.buttonStats:
                    onStatsClick();
                    break;
                case R.id.buttonHelp:
                    onHelpClick();
                    break;
            }
        }
        return true;
    }
}

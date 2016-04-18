package com.eys.blindcap;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.eys.ble.BluetoothHandler;

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

    ImageView frameAnimationHolder;
    AnimationDrawable frameAnimation;

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
    private ImageButton buttonMenu;

    Typeface fontRegular;


    private Context mContext;

    // Bluetooth
    private BluetoothHandler bluetoothHandler;
    private boolean beaconOn = false;
    private Runnable reconnectionRunnable;
    private Runnable vibrationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
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
        bluetoothHandler = BluetoothHandler.getInstance();
        bluetoothHandler.setContext(this);

        reconnectionRunnable = new Runnable() {
            @Override
            public void run() {
                Log.i("Runnable", "Reconnection runnable running");
                reconnect();
            }
        };

        vibrationRunnable = new Runnable() {
            @Override
            public void run() {
                Log.i("Runnable", "Turn runnable running");
                stopVibration();
            }
        };

    }

    public void reconnect(){
        if (!bluetoothHandler.isConnected() && !bluetoothHandler.reconnecting) {
            bluetoothHandler.reconnecting = true;
            showMessage("Trying to reconnect");
            bluetoothHandler.reconnect();
        }
        handler.removeCallbacks(reconnectionRunnable);
        handler.postDelayed(reconnectionRunnable, bluetoothHandler.reconnecting ? 2500 : 1000);
    }

    public MainActivity getMainActivity(){
        return this;
    }


    @Override
    protected void onStart() {
        super.onStart();

        //clearData();

        setFont();
        initStopWatch();
        initTurnButton();
        initMenu();
        handler.postDelayed(reconnectionRunnable, 1000);

    }



    @Override
    public void onPause(){
        super.onPause();
        stopTimer();
        handler.removeCallbacks(reconnectionRunnable);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        handler.removeCallbacks(reconnectionRunnable);
        bluetoothHandler.onDestroy();
    }


    @Override
    public void onResume(){
        super.onResume();
        handler.postDelayed(reconnectionRunnable, 1000);
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

        frameAnimationHolder = (ImageView) findViewById(R.id.arcsAnimation);
        frameAnimationHolder.setBackgroundResource(R.drawable.spinning_arcs_anim);
        frameAnimationHolder.setVisibility(View.GONE);
        frameAnimationHolder.setAlpha(0f);

        frameAnimation = (AnimationDrawable) frameAnimationHolder.getBackground();
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

        startArcsAnimation();
    }


    private void resetTimer() {
        time.set(0);
        updateTimerView();
    }


    private void stopTimer() {
        takeTimeSnapshot();

        handler.removeCallbacks(timerRunable);

        isTimerRunning = false;

        stopArcsAnimation();
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


    private void clearData() {
        timeSnapshots.clear();
        lapDataList.clear();
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


    private void startArcsAnimation() {
        //frameAnimation.selectDrawable(0);
        frameAnimation.start();

        frameAnimationHolder.setVisibility(View.VISIBLE);
        frameAnimationHolder.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);
    }


    private void stopArcsAnimation() {
        frameAnimationHolder.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        frameAnimation.stop();
                        frameAnimationHolder.setVisibility(View.GONE);
                    }
                });
    }


    //***************
    // Listeners
    //***************


    private void onTurnClick() {
        animOpacity.setTarget(turnButton);
        animOpacity.start();

        startVibration();

        takeTimeSnapshot();
    }

    private void stopVibration(){
        if(bluetoothHandler.isConnected() && beaconOn){
            bluetoothHandler.sendData(new byte[]{0});
            showMessage("Led OFF");
            beaconOn = false;
        }
    }

    private void startVibration(){
        if(bluetoothHandler.isConnected() && !beaconOn){
            bluetoothHandler.sendData(new byte[]{1});
            showMessage("Led ON");
            beaconOn = true;
            handler.postDelayed(vibrationRunnable, 3000);
        }
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

    private void showMessage(String str){
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }
}

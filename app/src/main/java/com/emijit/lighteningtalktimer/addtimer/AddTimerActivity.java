package com.emijit.lighteningtalktimer.addtimer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.emijit.lighteningtalktimer.MainActivity;
import com.emijit.lighteningtalktimer.R;
import com.emijit.lighteningtalktimer.data.Timer;

import com.emijit.lighteningtalktimer.data.TimerContract.TimerEntry;

public class AddTimerActivity extends AppCompatActivity implements TimerContract.AddTimerCallback {

    private static final String LOG_TAG = AddTimerActivity.class.getSimpleName();

    public static final String TIMER = "TIMER";
    private Toolbar mToolbar;
    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_timer);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // create a timer instance to live for the life of this Activity
        mTimer = new Timer();
        Bundle args = new Bundle();
        args.putParcelable(TIMER, mTimer);
        AddTimerFragment fragment = new AddTimerFragment();
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, fragment)
                .commit();
    }

    @Override
    public void forwardToSetInterval(Timer timer) {
        mToolbar.setTitle(getString(R.string.title_set_timer_interval));

        Bundle args = new Bundle();
        args.putParcelable(TIMER, timer);
        SetIntervalFragment fragment = new SetIntervalFragment();
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void setDoneBtn() {
        mTimer.setIntervals();
        this.getContentResolver().insert(
                TimerEntry.CONTENT_URI,
                mTimer.getContentValues()
        );
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public void setToolbarTitle() {
        mToolbar.setTitle(getString(R.string.title_activity_add_timer));
    }
}

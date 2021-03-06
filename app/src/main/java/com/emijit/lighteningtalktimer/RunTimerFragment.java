package com.emijit.lighteningtalktimer;

import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.emijit.lighteningtalktimer.data.Timer;
import com.emijit.lighteningtalktimer.data.TimerContract.TimerEntry;

import java.io.IOException;


public class RunTimerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = RunTimerFragment.class.getSimpleName();

    private View rootView;
    private Uri mUri;
    private long mTimerId = 0;
    private Timer mTimer;
    private RunTimerTask mRunTimerTask;

    private static final int DETAIL_LOADER = 0;

    public static String URI = "URI";

    public RunTimerFragment() {
    }

    static class ViewHolder {
        TextView intervals;
        TextView timerSeconds;
        TextView timerMinutes;
        TextView timerHours;

        public ViewHolder(View view) {
            intervals = (TextView) view.findViewById(R.id.run_timer_intervals);
            timerSeconds = (TextView) view.findViewById(R.id.timer_seconds);
            timerMinutes = (TextView) view.findViewById(R.id.timer_minutes);
            timerHours = (TextView) view.findViewById(R.id.timer_hours);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_run_timer, container, false);

        ViewHolder viewHolder = new ViewHolder(rootView);
        rootView.setTag(viewHolder);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(URI);
            if (mUri != null) {
                mTimerId = ContentUris.parseId(mUri);
                Log.d(LOG_TAG, "mTimerId: " + mTimerId);
            }
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mTimerId != 0) {
            return new CursorLoader(
                    getActivity(),
                    TimerEntry.CONTENT_URI,
                    null,
                    TimerEntry._ID + " = ?",
                    new String[] { Long.toString(mTimerId) },
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            mTimer = new Timer(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void startTimer() {
        mRunTimerTask = new RunTimerTask();
        mRunTimerTask.execute(mTimer);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRunTimerTask != null) {
            mRunTimerTask.cancel(true);
        }
    }

    private class RunTimerTask extends AsyncTask<Timer, Integer, Long> {

        private final String LOG_TAG = RunTimerTask.class.getSimpleName();

        Timer mTimer;
        SoundPool mSoundPool;
        int mCurrentSeconds = 0;
        int mCurrentIntervals = 0;
        int alertSoundId;

        @Override
        protected Long doInBackground(Timer... params) {
            Log.d(LOG_TAG, "doInBackground");
            initSounds();
            mTimer = params[0];

            while (mTimer.getIntervals() > mCurrentIntervals) {
                try {
                    Thread.sleep(1000);
                    mCurrentSeconds++;
                    publishProgress(mCurrentSeconds);
                } catch (InterruptedException e) {
                    Log.d(LOG_TAG, e.toString());
                    break;
                }
            }
            return (long) mCurrentIntervals;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            int interval = values[0];
            String strInterval = Integer.toString(interval);
            Log.d(LOG_TAG, "onProgressUpdate: " + strInterval);

            // update view w/ current interval
            ViewHolder viewHolder = (ViewHolder) rootView.getTag();

            // intervals
            if (mCurrentSeconds % mTimer.getIntervalSeconds().getRawSeconds() == 0) {
                mCurrentIntervals++;
                viewHolder.intervals.setText(Integer.toString(mCurrentIntervals));

                if (mCurrentIntervals != mTimer.getIntervals()) {
                    alertSound();
                }
            }

            // hour/min/sec timer
            if (mCurrentSeconds <= mTimer.getTimerSeconds().getRawSeconds()) {
                viewHolder.timerSeconds.setText(String.format("%02d", mCurrentSeconds % 60));
                if (mCurrentSeconds >= 60) {
                    viewHolder.timerMinutes.setText(String.format("%02d", mCurrentSeconds / 60));
                    viewHolder.timerHours.setText(String.format("%02d", mCurrentSeconds / 3600));
                }
            }
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            Log.d(LOG_TAG, "onPostExecute");

            for (int i = 0; i < 4; i++) {
                alertSound();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.d(LOG_TAG, e.toString());
                }
            }
        }

        private void initSounds() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes =
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build();

                mSoundPool = new SoundPool.Builder()
                        .setMaxStreams(5)
                        .setAudioAttributes(audioAttributes)
                        .build();
            } else {
                mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
            }

            try {
                AssetManager assetManager = getActivity().getAssets();
                AssetFileDescriptor descriptor;

                descriptor = assetManager.openFd("beep.ogg");
                alertSoundId = mSoundPool.load(descriptor, 0);
            } catch (IOException e) {
                Log.d("MainActivity", "sound asset error");
            }

            mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    Log.d(LOG_TAG, "sound onLoadComplete: " + sampleId);
                }
            });
        }

        private void alertSound() {
            mSoundPool.play(alertSoundId, 1, 1, 0, 0, 1);
        }
    }
}

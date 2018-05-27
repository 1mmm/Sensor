package com.example.a11324.sensor;

/**
 * Created by 2mmm on 2017/7/10.
 */


import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;


public class SoundService extends Service {

    private MediaPlayer mp;



    @Override

    public void onCreate() {

        super.onCreate();

        mp = MediaPlayer.create(this, R.raw.a11);

    }



    @Override

    public void onDestroy() {

        super.onDestroy();

        mp.release();

        stopSelf();

    }



    @Override

    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean playing = intent.getBooleanExtra("playing", false);

        if (playing) {
            Log.d("shengyin", "bofang ");

            mp.start();

        } else {

            mp.pause();

        }

        return super.onStartCommand(intent, flags, startId);

    }



    @Override

    public IBinder onBind(Intent intent) {

        return null;

    }



}



package com.example.a11324.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by 11324 on 2017/4/30.
 * 开机启动的类
 */

public class MyBootReceiver extends BroadcastReceiver {
    private Intent service_intent = null;
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Log.d("MyBootReceiverTAG", "boot complete hahaha!");
            Toast.makeText(context, "boot completed", Toast.LENGTH_SHORT).show();
            service_intent = new Intent(context,MainActivity.class);
            service_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //service_intent.setPackage("com.example.a11324.sensor");
            //service_intent.putExtra("file_name", "");
            //service_intent.putExtra("HOST", "10.133.156.94");

            context.startActivity(service_intent);
        }
    }
}

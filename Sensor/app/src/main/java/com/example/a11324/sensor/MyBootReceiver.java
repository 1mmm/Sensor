package com.example.a11324.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Created by 11324 on 2017/4/30.
 * 开机启动的类
 */

public class MyBootReceiver extends BroadcastReceiver {
    private Intent service_intent = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MyBootReceiverTAG","boot complete hahaha!");
        Toast.makeText(context,"boot completed",Toast.LENGTH_SHORT).show();
        service_intent = new Intent(context,SensorWrite.class);
        service_intent.setPackage("com.example.a11324.sensor");
        service_intent.putExtra("file_name",getFileName());             //确定名字
        service_intent.putExtra("HOST","10.133.156.94");
        context.startService(service_intent);
    }
    private String getFileName(){
        String fileName = null;
        File dir = new File("/sdcard/GaitTXT/");
        String[] dirList = dir.list();
        if (dirList.length > 0)
            fileName = dirList[0];
        fileName = fileName.substring(0,fileName.length()-4);
        Log.d("MyBootReceiverTAG",fileName);
        return fileName;
    }
}

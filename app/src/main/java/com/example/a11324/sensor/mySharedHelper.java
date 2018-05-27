package com.example.a11324.sensor;

/**
 * Created by 2mmm on 2017/5/7.
 */

import android.content.Context;
import android.content.SharedPreferences;

public class mySharedHelper {

    private Context mContext;
    private SharedPreferences.Editor editor;
    private SharedPreferences sp;

    public mySharedHelper(Context mContext) {
        this.mContext = mContext;
        sp = mContext.getSharedPreferences("mysp", Context.MODE_PRIVATE);
        editor = sp.edit();
    }


    //定义一个保存数据的方法
    public void save(String key,String value) {                //
        editor.putString(key,value);
        editor.commit();
    }

    public  String read(String key){
        return sp.getString(key,"default");
    }
}

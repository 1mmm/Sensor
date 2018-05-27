package com.example.a11324.sensor;

/**
 * Created by 11324 on 2017/4/11.
 */

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class LogActivity extends Activity{
    TextView welcomeView;
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        findviews();
        welcomeView.setText("欢迎"+MainActivity.file_name);
    }

    public void findviews(){
        welcomeView=(TextView)findViewById(R.id.welcome);
    }
}
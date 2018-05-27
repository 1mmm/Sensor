package com.example.a11324.sensor;

/**
 * Created by 11324 on 2017/4/11.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;


public class MainActivity extends Activity {

    private SensorWrite.MessageBinder messageBinder;    //服务器通信类
    static  TextView Acc_x ;
    static TextView Acc_y ;
    static TextView Acc_z ;
    TextView labelView;                                 //

    private Button startWalk_button;
    private Button stop_button;
    private Button connect;
    private Button register;
    private EditText fileName_editText;                                         //显示文件名的editText
    private EditText host_editText;


    LinearLayout layout;

    static String file_name;                                                     //文件名
    String HOST;                                                                //主机名
    Socket socket=null;
    boolean isBind = false;
    public ServiceConnection connection = new ServiceConnection() {             //绑定服务
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messageBinder = (SensorWrite.MessageBinder) service;
            boolean isBind = true;
            Log.d("MainActivity","bind service succeed");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boolean isBind = false;
        }
    };

    protected int flag=1;
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                         //例行公事
        layout=(LinearLayout) findViewById(R.id.layout);
        LinearLayout layout1=(LinearLayout) findViewById(R.id.layout1);

        layout1.getBackground().setAlpha(180);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
                .build());

        new Thread(networkTask).start();                                //

    }
    Runnable networkTask = new Runnable() {

        @Override
        public void run() {
            findviews();
            setonclick();
        }
    };


    public void findviews() {
        labelView=(TextView)findViewById(R.id.textView);
        fileName_editText =(EditText)findViewById(R.id.fileName_editText);
        host_editText = (EditText)findViewById(R.id.host);
        checksdcard();
        Acc_x= (TextView)findViewById(R.id.Acc_x);
        Acc_y= (TextView)findViewById(R.id.Acc_y);
        Acc_z= (TextView)findViewById(R.id.Acc_z);
        layout=(LinearLayout) findViewById(R.id.layout);
        startWalk_button = (Button) findViewById(R.id.button1);             //绑定四个按钮
        stop_button = (Button) findViewById(R.id.button2);
        connect = (Button)this.findViewById(R.id.connect) ;
        register = (Button)this.findViewById(R.id.register) ;
        connect.setEnabled(false);
        stop_button.setEnabled(false);
        register.setEnabled(false);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0X11);
            //判断是否需要 向用户解释，为什么要申请该权限
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_CONTACTS);
            //InputMethodManager  manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        Log.d("MainActivity","find_view all right");
    }
    public boolean onTouchEvent(MotionEvent event) {
        if(null != this.getCurrentFocus()){
            /**
             * 点击空白位置 隐藏软键盘
             */
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super .onTouchEvent(event);
    }

    public void setonclick(){
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    socket=messageBinder.connecttoserver();
                    Log.d("MainActivity","connect  all right");
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if(socket!=null)
                    Toast.makeText(MainActivity.this,"connect to server!",Toast.LENGTH_SHORT).show();
                else
                    showVeryResult("很抱歉，您还未注册！");
            }
        });
        register.setOnClickListener(new Button.OnClickListener()
        {
            public void onClick(View v) {
                try {
                    socket=messageBinder.connecttoserver2();
                    Log.d("MainActivity","connect  all right");
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        startWalk_button.setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onStartWalking();
                connect.setEnabled(true);
                stop_button.setEnabled(true);
            }
        });
        stop_button.setOnClickListener(new Button.OnClickListener()
        {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                flag=1;
                LinearLayout layout=(LinearLayout) findViewById(R.id.layout);

                layout.setBackgroundResource(R.drawable.background);
                messageBinder.stop();
                Toast toast=Toast.makeText(MainActivity.this,"file saved!",Toast.LENGTH_SHORT);
                toast.setGravity(toast.getGravity(), 0, 600);
                toast.show();
            }
        });
    }

    //----------------------------------写数据相关---------------------------------------------
    protected void onStartWalking(){
        layout.setBackgroundColor(Color.parseColor("#B2DFEE"));
        Toast toast=Toast.makeText(MainActivity.this,"start walking!",Toast.LENGTH_SHORT);
        toast.setGravity(toast.getGravity(), 0, 600);
        toast.show();
        //开启活动
        file_name = fileName_editText.getText().toString();
        HOST = host_editText.getText().toString();
        Intent intent = new Intent(MainActivity.this,SensorWrite.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);
        intent.putExtra("file_name",file_name);
        intent.putExtra("HOST",HOST);
        startService(intent);
        Log.d("MainActivity",file_name);
        Log.d("MainActivity",HOST);
    }
    protected void onPause() {
        super.onPause();
    }
    public void checksdcard(){                              //查看sd卡的文件模式
        //  boolean mExternalStorageAvailable = false;
        //boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            // 对SD 卡上的存储可以进行读/写操作
            labelView.setText("对SD 卡上的存储可以进行读/写操作");
            //   mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else
        if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY))
        {
            //对SD 卡上的存储可以进行读操作
            labelView.setText("对SD 卡上的存储可以进行读操作");
            //    mExternalStorageAvailable = true;
            //     mExternalStorageWriteable = false;
        } else {
            //对SD 卡上的存储不可用
            labelView.setText("对SD 卡上的存储不可用");
            //    mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
    }
    //----------------------------------这啥玩意-------------------------------------------------
    private void showVeryResult(String msg){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("哎呀")
                .setMessage(msg)
                .setPositiveButton("知道啦", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which){
                        return;
                    }
                }).create(); //创建对话框
        alertDialog.show(); // 显示对话框
    }
    private void showTips(){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("EXIT")
                .setMessage("Are you sure to exit?")
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which){
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which){
                                return;
                            }}).create(); //创建对话框
        alertDialog.show(); // 显示对话框
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0){
            this.showTips();
            return false;
        }
        return false;
    }
    //------------------------------------------------------------------------------------------这个是退出键的监听弹出框
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        unbindService(connection);
        super.onDestroy();
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
        MainActivity.this.finish();
    }
}

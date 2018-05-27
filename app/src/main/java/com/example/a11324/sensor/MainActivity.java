package com.example.a11324.sensor;

/**
 * Created by 11324 on 2017/4/11.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;

import static android.util.Log.d;


public class MainActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;
    private final String tag = "zhangphil";
    private final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private SensorWrite.MessageBinder messageBinder;    //服务器通信类
    static TextView Acc_x ;
    static TextView Acc_y ;
    static TextView Acc_z ;
    static TextView Gyr_x ;
    static TextView Gyr_y ;
    static TextView Gyr_z ;
    static TextView last_time;
    public boolean ft=false;
    static TextView labelView,a1;                                 //
    String ch;
    String ssseq;
    double Longitude,Latitude;
    mySharedHelper sh;
    private Context mContext;
    private ProgressDialog pd1 = null;
    public Button startWalk_button;
    private Button stop_button;
    private Button connect;
    private Button register;
    private boolean ff=false;
    private Button sl;
    private Handler hand=null;
    private EditText fileName_editText;                                         //显示文件名的editText
    private EditText host_editText;
    GPSLocationManager gpsLocationManager;
    private BroadcastReceiver MybroadcastReceiver = null;


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
            d("MainActivity","bind service succeed");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boolean isBind = false;
        }
    };

    protected int flag=1;
    private void showMsg(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    showMsg("读取失败");
                    break;
                case 2:
                    showMsg("读取成功");
                    String receive = msg.obj + "";
                    labelView.setText(receive);
                    if (!ft) startWalk_button.performClick();
                    break;
            }
            return false;
        }
    });
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @SuppressLint("NewApi")
    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                         //例行公事
        layout=(LinearLayout) findViewById(R.id.layout);
        hand=new Handler();
        LinearLayout layout1=(LinearLayout) findViewById(R.id.layout1);
        mContext=getApplicationContext();
        sh=new mySharedHelper(mContext);
        ch=sh.read("hhh");
        layout1.getBackground().setAlpha(180);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
                .build());

        new Thread(networkTask).start();

        MybroadcastReceiver =  new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                String time = intent.getStringExtra("last_time");
                last_time.setText(time);
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ACTION_LAST_TIME");
        registerReceiver(MybroadcastReceiver, intentFilter);

    }
    Runnable networkTask = new Runnable() {

        @Override
        public void run() {
            findviews();
            setonclick();
        }
    };
    Runnable   runnableUi=new  Runnable(){
        @Override
        public void run() {
            //更新界面
            a1.setText("经度：" + Longitude + " 纬度：" + Latitude);
        }

    };
    class MyListener implements GPSLocationListener {

        @Override
        public void UpdateLocation(Location location) {
            d("gps", "GPS All right!");
            if (location != null) {
                Longitude=location.getLongitude();
                Latitude=location.getLatitude();
                hand.post(runnableUi);
            }
        }

        @Override
        public void UpdateStatus(String provider, int status, Bundle extras) {
            if ("gps" == provider) {
                Toast.makeText(MainActivity.this, "定位类型：" + provider, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void UpdateGPSProviderStatus(int gpsStatus) {
            switch (gpsStatus) {
                case GPSProviderStatus.GPS_ENABLED:
                    Toast.makeText(MainActivity.this, "GPS开启", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_DISABLED:
                    Toast.makeText(MainActivity.this, "GPS关闭", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_OUT_OF_SERVICE:
                    Toast.makeText(MainActivity.this, "GPS不可用", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_TEMPORARILY_UNAVAILABLE:
                    Toast.makeText(MainActivity.this, "GPS暂时不可用", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_AVAILABLE:
                    Toast.makeText(MainActivity.this, "GPS可用啦", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String name = device.getName();
                if (name != null)
                    Log.d(tag, "发现设备:" + name);

                if (name != null && name.equals("HC-06")) {
                    Log.d(tag, "发现目标设备，开始线程连接!");

                    // 蓝牙搜索是非常消耗系统资源开销的过程，一旦发现了目标感兴趣的设备，可以考虑关闭扫描。
                    mBluetoothAdapter.cancelDiscovery();

                    new Thread(new ClientThread(device)).start();
                }
            }
        }
    };

    private class ClientThread extends Thread {

        private BluetoothDevice device;

        public ClientThread(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {

            BluetoothSocket socket = null;

            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

                Log.d(tag, "连接服务端...");
                socket.connect();
                Log.d(tag, "连接建立.");
                InputStream inputStream = socket.getInputStream();
                String data;
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        inputStream.read(buffer);

                        data = new String(buffer);
                        Message msg = mHandler.obtainMessage();
                        msg.what = 2;
                        msg.obj = socket.getRemoteDevice().getName() + "    " + data;
                        mHandler.sendMessage(msg);
                    } catch (IOException e) {
                        mHandler.sendEmptyMessage(1);
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void findviews() {
        labelView=(TextView)findViewById(R.id.textView);
        fileName_editText =(EditText)findViewById(R.id.fileName_editText);
        fileName_editText.setText(sh.read("last_name"));
        host_editText = (EditText)findViewById(R.id.host);
        checksdcard();
        Acc_x= (TextView)findViewById(R.id.Acc_x);
        Acc_y= (TextView)findViewById(R.id.Acc_y);
        Acc_z= (TextView)findViewById(R.id.Acc_z);
        Gyr_x= (TextView)findViewById(R.id.Gyr_x);
        Gyr_y= (TextView)findViewById(R.id.Gyr_y);
        Gyr_z= (TextView)findViewById(R.id.Gyr_z);
        last_time= (TextView)findViewById(R.id.last_time);
        last_time.setText(sh.read("last_record_time"));
        a1=(TextView)findViewById(R.id.a1);
        sl=(Button)findViewById(R.id.sl);
        sl.setText("<");
        layout=(LinearLayout) findViewById(R.id.layout);
        startWalk_button = (Button) findViewById(R.id.button1);             //绑定四个按钮
        stop_button = (Button) findViewById(R.id.button2);
        connect = (Button)this.findViewById(R.id.connect) ;
        register = (Button)this.findViewById(R.id.register) ;
        connect.setEnabled(true);
        stop_button.setEnabled(false);
        register.setEnabled(true);
        gpsLocationManager = GPSLocationManager.getInstances(MainActivity.this);

        if (ContextCompat.checkSelfPermission(this,
               android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
               != PackageManager.PERMISSION_GRANTED) {//请求权限
            ActivityCompat.requestPermissions(this,
                   new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0X11);
           //判断是否需要 向用户解释，为什么要申请该权限
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                   android.Manifest.permission.READ_CONTACTS);
           InputMethodManager  manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        d("MainActivity","find_view all right");
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
        sl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ff){
                    a1.setVisibility(View.GONE);
                    ff=!ff;
                    sl.setText("...");
                }
                else{
                    a1.setVisibility(View.VISIBLE);
                    ff=!ff;
                    sl.setText("<");
                }
            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    HOST = host_editText.getText().toString();
                    socket=messageBinder.connecttoserver();
                    d("MainActivity","connect  all right");
                } catch (UnknownHostException e) {
                    d("MainActivity","connect  error");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    d("MainActivity","connect  error");
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
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                // 注册广播接收器。接收蓝牙发现讯息
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter);

                if (mBluetoothAdapter.startDiscovery()) {
                    Log.d(tag, "启动蓝牙扫描设备...");
                }
            }
        });

        startWalk_button.setOnClickListener(new Button.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onStartWalking();
                ft=true;
                connect.setEnabled(true);
                stop_button.setEnabled(true);
                startWalk_button.setEnabled(false);
                register.setEnabled(true);
                try
                {
                    Thread.currentThread().sleep(1000);//毫秒
                    gpsLocationManager.start(new MyListener());
                }
                catch(Exception e){}

            }
        });
        stop_button.setOnClickListener(new Button.OnClickListener()
        {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                flag=1;
                LinearLayout layout=(LinearLayout) findViewById(R.id.layout);
                ft=false;
                layout.setBackgroundResource(R.drawable.background);
                messageBinder.stop();
                Toast toast=Toast.makeText(MainActivity.this,"file saved!",Toast.LENGTH_SHORT);
                toast.setGravity(toast.getGravity(), 0, 600);
                toast.show();
                startWalk_button.setEnabled(true);
                stop_button.setEnabled(false);
            }
        });


    }

    //----------------------------------写数据相关---------------------------------------------
    protected void onStartWalking(){
        layout.setBackgroundColor(Color.parseColor("#B2DFEE"));
//        Toast toast=Toast.makeText(MainActivity.this,"start walking!",Toast.LENGTH_SHORT);
//        toast.setGravity(toast.getGravity(), 0, 600);
//        toast.show();
        //开启活动
        file_name = fileName_editText.getText().toString();

        HOST = host_editText.getText().toString();
        Intent intent = new Intent(MainActivity.this,SensorWrite.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);
        intent.putExtra("file_name",file_name);
        intent.putExtra("HOST",HOST);

        startService(intent);






        d("MainActivity",file_name);
        d("MainActivity",HOST);
    }

    protected void onPause() {
        super.onPause();
        gpsLocationManager.stop();
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
//    private void setLast_time(){
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {                    //每30秒判断是否写文件
//            @Override
//            public void run() {
//                last_time.setText(SensorWrite.last_record_time);
//                Log.d("last_time",SensorWrite.last_record_time);
//            }
//        }, 30000, 30000);
//    }
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
        d("kill","activity was killed");
        unbindService(connection);
        unregisterReceiver(mReceiver);
        if (isBind){
            Intent intent = new Intent("com.example.a11324.sensor.restart_service");
            sendBroadcast(intent);
        }
        super.onDestroy();
    }
}

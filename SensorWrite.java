package com.example.a11324.sensor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.a11324.sensor.MainActivity.Acc_x;
import static com.example.a11324.sensor.MainActivity.Acc_y;
import static com.example.a11324.sensor.MainActivity.Acc_z;
import static com.example.a11324.sensor.MainActivity.Gyr_x;
import static com.example.a11324.sensor.MainActivity.Gyr_y;
import static com.example.a11324.sensor.MainActivity.Gyr_z;
import static com.example.a11324.sensor.MainActivity.last_time;


/**
 * Created by 11324 on 2017/4/23.
 */

public class SensorWrite extends Service implements SensorEventListener {
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            setLastTime();
            return false;
        }
    });
    SensorManager sensorManager;                                                                                                //传感器manager
    Sensor Acc,Gyr,Pres,Gra;
    MediaPlayer mp =new MediaPlayer();;//三个传感器，加速度，陀螺仪和压力
    static String timeData, SensorData_Acc ="", SensorData_Gyr ="", SensorData_Pre ="";                                         //四个数据
    mySharedHelper sh;
    int ch;
    File dir = null;
    StringBuffer stringBuf = new StringBuffer();
    float max_x = 0,min_x = 0;
    float max_y = 0,min_y = 0;
    float max_z = 0,min_z = 0;
    float ac_x,ac_y,ac_z;
    float gr_x = 0,gr_y = 0,gr_z = 0;
    protected static String last_record_time = "";
    private Context mContext;
    int step = 0;
    static Socket socket=null;
    private static InputStream is=null;
    BufferedWriter writer=null;
    BufferedReader reader=null;
    String file_name,file_name1;                                                                                                //新建文件的名字
    String file_seq,infile_seq;
    String HOST;                                                                                                                //连接服务器的地址
    private IntentFilter ScreenOff_intentFilter;                                                                                //用于注册熄灭屏幕广播
    private BroadcastReceiver ScreenOff_receiver;                                                                               //接收熄灭屏幕广播
    private boolean isReg = false;                                                                                              //判断是否注册了监听器
    private PowerManager powerManager;                                                                                          //设置电源管理者
    private PowerManager.WakeLock mWakeLock;                                                                                    //CPU 运行锁

    private MessageBinder mBinder ;                                                                                             //传递消息的类

    @Override
    public void onCreate() {
        Log.d("SensorWrite","onCreate");
        super.onCreate();
        mp.setLooping(true);
        mp = MediaPlayer.create(this, R.raw.d0);
    }
    @Override
    public int onStartCommand(Intent intent,  int flags, int startId) {
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);                                                  //设置电源管理者
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SensorWrite");                                     //CPU 运行锁
        mBinder = new MessageBinder();
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        file_name = intent.getStringExtra("file_name");                                                                         //获得传过来的文件名
        HOST = intent.getStringExtra("HOST");
        getFileName();
        createDir();
        Log.d("SensorWrite",file_name);
        Log.d("SensorWrite",HOST);
        start();
        boolean playing = intent.getBooleanExtra("playing", false);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer arg0) {
                mp.start();
                mp.setLooping(true);
            }
        });
        ScreenOff_intentFilter = new IntentFilter();
        ScreenOff_intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        ScreenOff_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("SensorWrite","on receive : screen off!");
                if(isReg){
                    sensorManager.unregisterListener(SensorWrite.this);
                    RegisterForGyr();
                    RegisterForAcc();
                    RegisterForPres();
                    Log.d("SensorWrite","re registerListener");
                }
            }
        };
        registerReceiver(ScreenOff_receiver,ScreenOff_intentFilter);

        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        Log.d("kill","service was killed");
        mWakeLock.release();
        sensorManager.unregisterListener(this);
        unregisterReceiver(ScreenOff_receiver);
        super.onDestroy();

        mp.stop();
        mp.release();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("SensorWrite","onBind");
        mBinder = new MessageBinder();
        return mBinder;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
    @Override
    public void onSensorChanged(SensorEvent event) {                            //定义了传感器变化时的动作
        synchronized (this) {
            switch(event.sensor.getType())
            {
                case Sensor.TYPE_ACCELEROMETER:
                {
                    if(ac_x > max_x)
                        max_x = ac_x;
                    if(ac_x < min_x)
                        min_x = ac_x;
                    if(ac_y > max_y)
                        max_y = ac_y;
                    if(ac_y < min_y)
                        min_y = ac_y;
                    if(ac_z > max_z)
                        max_z = ac_z;
                    if(ac_z < min_z)
                        min_z = ac_z;
                }
                // In this example, alpha is calculated as t / (t + dT),
                // where t is the low-pass filter's time-constant and
                // dT is the event delivery rate.

                final float alpha = 0.8f;

                // Isolate the force of gravity with the low-pass filter.
                gr_x = alpha * gr_x + (1 - alpha) * event.values[0];
                gr_y = alpha * gr_y + (1 - alpha) * event.values[1];
                gr_z = alpha * gr_z + (1 - alpha) * event.values[2];

                // Remove the gravity contribution with the high-pass filter.
                float pure_x,pure_y,pure_z;
                pure_x = event.values[0] - gr_x;
                pure_y = event.values[1] - gr_y;
                pure_z = event.values[2] - gr_z;
                setAccelerometer(event);
                if(SensorData_Acc !=""){
                    SensorData_Acc = SensorData_Acc+pure_x+" "+pure_y+" "+pure_z+" ";
                    stringBuf.append(SensorData_Acc);
                    stringBuf.append("\n");
                }
                break;
                case Sensor.TYPE_GYROSCOPE:
                    setGyroscope(event);
                    if(SensorData_Gyr !="")
                        //SensorData_Gyr="gyr!"+SensorData_Gyr+" ";
                        //stringBuf.append(SensorData_Gyr);
                        //stringBuf.append("\n");
                    break;
                case Sensor.TYPE_GRAVITY:
                    setGravity(event);
                    break;
            }
            Log.d("3test",String.valueOf(ac_x));
        }
    }

    protected void start(){
        RegisterForAcc();
        RegisterForGyr();
        RegisterForPres();
        RegisterForGra();
        isReg = true;
        mWakeLock.acquire();
        SetTimeAction();
    }                                           //里面注册了传感器
    private void setPressure(SensorEvent event) {
        float pre = event.values[0];
        SensorData_Pre =pre+" \r\n";
    }                     //获得各个数据
    private void setGyroscope(SensorEvent event) {
        float gy_x = event.values[sensorManager.DATA_X];
        float gy_y = event.values[sensorManager.DATA_Y];
        float gy_z = event.values[sensorManager.DATA_Z];
        Gyr_x.setText("X=     "+gy_x);
        Gyr_y.setText("Y=     "+gy_y);
        Gyr_z.setText("Z=     "+gy_z);
        SensorData_Gyr =gy_x+" "+gy_y+" "+gy_z;
    }
    private void setAccelerometer(SensorEvent event) {
        ac_x = event.values[sensorManager.DATA_X];
        ac_y = event.values[sensorManager.DATA_Y];
        ac_z = event.values[sensorManager.DATA_Z];
        Acc_x.setText("X=     "+ac_x);
        Acc_y.setText("Y=     "+ac_y);
        Acc_z.setText("Z=     "+ac_z);
        SensorData_Acc =ac_x+" "+ac_y+" "+ac_z+" ";
    }
    private void setGravity(SensorEvent event){
        gr_x = event.values[0];
        gr_y = event.values[1];
        gr_z = event.values[2];
    }
    private void RegisterForGyr() {
        Gyr = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, Gyr, 20000);
    }
    private void RegisterForAcc() {
        Acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, Acc,  20000);
    }
    private void RegisterForPres() {
        Pres = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(this, Pres,  20000);
    }
    private void RegisterForGra(){
        Gra = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this,Gra, 20000);
    }
    //----------------------------------写数据相关---------------------------------------------
    private void createDir(){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);// 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
        }
        String path_str=sdDir.getPath()+"/GaitTXT";
        dir = new File(path_str);
        if (!dir.exists()) {
            //若不存在，创建目录，可以在应用启动的时候创建
            boolean ismake = dir.mkdirs();
            Log.d("yyyyy",String.valueOf(ismake));
        }
    }
    private void getFileName(){
        mContext=getApplicationContext();
        sh=new mySharedHelper(mContext);
        if(file_name.equals("")){                                       //如果没有传入名字，默认用上一个的名字
            Log.d("seq","just");
            file_name = sh.read("last_name");
            Log.d("seq",file_name);
        }

        file_seq = sh.read(file_name+"_number");
        infile_seq = sh.read(file_name+"_in_number");
        Log.d("seq",file_seq);
        Log.d("in_seq",infile_seq);
        if(file_seq.equals("default")){                              //说明没有这个人的记录
            file_seq = ""+1;
            infile_seq = ""+1;
        }
    }
    private String gettime() {
        Calendar ca = Calendar.getInstance();
        timeData = DateFormat.format("yyyyMMddkkmmss",ca.getTime()).toString()+" ";
        return timeData;
    }
    private void SetTimeAction(){
        Log.d("SensorWrite","I am in setTime");
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {                    //每1秒判断是否正在走路
            @Override
            public void run() {                             //判断条件 极值
                Log.d("SensorWrite","per second step = "+step);
                boolean isStep = true;
                if((max_x-min_x)<0.1)
                    if((max_y-min_y)<0.1)
                        if((max_z-min_z)<0.1)
                            //isStep = true;
                            isStep=false;
                if(isStep)
                    step++;
                max_x = -50;                                  //每秒最大最小值都清空
                max_y = -50;
                max_z = -50;
                min_x = 50;
                min_y = 50;
                min_z = 50;
            }
        }, 500, 500);                                     //延迟一秒进行，每秒一次
        timer.schedule(new TimerTask() {                    //每30秒判断是否写文件
            @Override
            public void run() {
                Log.d("SensorWrite","per half minute,step = "+step);
                if(step > 1){
                    mHandler.sendEmptyMessage(1);
                    writedata(stringBuf.toString());
                    Log.d("SensorWrite","I am in local write");
                    if(socket != null){                     //如果连上了，就返回
                        try {
                            writer.write(stringBuf.toString()+" ");
                            writer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                step = 0;
                stringBuf.setLength(0);
            }
        }, 3000, 3000);
    }
    void setLastTime(){
        last_time.setText(gettime());
    }
    private void writedata(String string){

        String fileName = null;
            fileName = file_name+".txt";

        if (!dir.exists())
            dir.mkdir();

        if (dir.exists() && dir.canWrite()) {
            File newFile = new File(dir.getAbsolutePath() + "/" + fileName);
            FileOutputStream fos = null;
            try {
                newFile.createNewFile();
                if (newFile.exists() && newFile.canWrite()) {
                    fos = new FileOutputStream(newFile,true);
                    fos.write((string+"\n").getBytes());

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try{
                        fos.flush();
                        fos.close();
                    }
                    catch (IOException e) { }
                }
            }
        }
    }
    private Socket RequestSocket(String host,int port) throws UnknownHostException, IOException {
        Socket socket = new Socket(host, port);
        return socket;
    }
    class MessageBinder extends Binder{
        public void stop() {
            if (isReg){
                sensorManager.unregisterListener(SensorWrite.this); // 解除监听器注册
                isReg = false;
                Log.d("SensorWrite","解除监听器注册");
            }
            if (mWakeLock.isHeld()){
                mWakeLock.release();
            }
        }
        //---------------------------------连接服务器相关------------------------------------------
        public Socket connecttoserver() throws UnknownHostException, IOException {
            Log.d("host", HOST);
            Socket a_socket = RequestSocket(HOST,12345);
            writer = new BufferedWriter(new OutputStreamWriter(a_socket.getOutputStream()));
            is=a_socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            //file_name1 = "#"+file_name+file_seq+"\n";
            //writer.write(file_name1);
            //Log.d("SensorWrite",file_name1);
            //writer.flush();
            SensorWrite.socket = a_socket;
            return a_socket;
        }
        public Socket connecttoserver2() throws UnknownHostException, IOException {
            Socket socket=RequestSocket(HOST,5000);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            is=socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));
            writer.write("R_"+file_name);
            writer.flush();
            return socket;
        }
    }
}

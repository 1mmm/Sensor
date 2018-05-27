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
import android.os.Binder;
import android.os.IBinder;
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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.a11324.sensor.MainActivity.Acc_x;
import static com.example.a11324.sensor.MainActivity.Acc_y;
import static com.example.a11324.sensor.MainActivity.Acc_z;

/**
 * Created by 11324 on 2017/4/23.
 */

public class SensorWrite extends Service implements SensorEventListener {
    SensorManager sensorManager;                                                                                                //传感器manager
    Sensor Acc,Gyr,Pres;                                                                                                        //三个传感器，加速度，陀螺仪和压力
    static String timeData, SensorData_Acc ="", SensorData_Gyr ="", SensorData_Pre ="";                                                //四个数据
    SharedHelper sh;
    int ch;
    File dir = new File("/sdcard/GaitTXT/");
    StringBuffer stringBuf = new StringBuffer();
    float max_x = 0,min_x = 0;
    float max_y = 0,min_y = 0;
    float max_z = 0,min_z = 0;
    float ac_x,ac_y,ac_z;
    private Context mContext;
    int step = 0;
    static Socket socket=null;
    private static InputStream is=null;
    BufferedWriter writer=null;
    BufferedReader reader=null;
    String file_name,file_name1;                                                                                                           //新建文件的名字
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
    }
    @Override
    public int onStartCommand(Intent intent,  int flags, int startId) {
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);                                                  //设置电源管理者
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SensorWrite");                                     //CPU 运行锁
        mBinder = new MessageBinder();
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        file_name = intent.getStringExtra("file_name");
        HOST = intent.getStringExtra("HOST");
        Log.d("SensorWrite",file_name);
        Log.d("SensorWrite",HOST);
        start();

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

        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
        sensorManager.unregisterListener(this);
        unregisterReceiver(ScreenOff_receiver);
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
                setAccelerometer(event);
                if(SensorData_Acc !=""){
                    stringBuf.append(gettime());
                    stringBuf.append(SensorData_Acc);
                    stringBuf.append("\n");
                }
                break;
                case Sensor.TYPE_GYROSCOPE:
                    setGyroscope(event);
                    if(SensorData_Gyr !="")
                        stringBuf.append(SensorData_Gyr);
                    break;
                case Sensor.TYPE_PRESSURE:
                    setPressure(event);
                    if(SensorData_Pre !="")
                        stringBuf.append(SensorData_Pre);
                    break;
            }
        }
    }

    protected void start(){
        RegisterForAcc();
        RegisterForGyr();
        RegisterForPres();
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
        SensorData_Gyr =gy_x+" "+gy_y+" "+gy_z+" ";
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
    //----------------------------------写数据相关---------------------------------------------
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
                if((max_x-min_x)<7)
                    if((max_y-min_y)<7)
                        if((max_z-min_z)<7)
                            isStep = false;
                if(isStep)
                    step++;
                max_x = 0;                                  //每秒最大最小值都清空
                max_y = 0;
                max_z = 0;
                min_x = 0;
                min_y = 0;
                min_z = 0;
            }
        }, 1000, 1000);                                     //延迟一秒进行，每秒一次
        timer.schedule(new TimerTask() {                    //每30秒判断是否写文件
            @Override
            public void run() {
                Log.d("SensorWrite","per half minute,step = "+step);
                if(step > 25){
                    writedata(stringBuf.toString());
                    Log.d("SensorWrite","I am in local write");
                    if(socket != null){                     //如果连上了，就返回
                        try {
                            writer.write(stringBuf.toString());
                            writer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                step = 0;
                stringBuf.setLength(0);
            }
        }, 30000, 30000);
    }
    private void writedata(String string){
        mContext=getApplicationContext();
        sh=new SharedHelper(mContext);
        Map<String,String> data = sh.read(file_name);
        if (data.get(file_name).equals("")) sh.save(file_name,"1");
        else
        {
            ch=Integer.parseInt(data.get(file_name));
            if (ch==120) {
                file_name = file_name + "+";
                sh.save(file_name, "1");
                ch=1;
            }
            else
            {
                ch=ch+1;
                sh.save(file_name,""+ch);
            }
        }


        String fileName =file_name+".txt";

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
            Socket a_socket = RequestSocket(HOST,5000);
            writer = new BufferedWriter(new OutputStreamWriter(a_socket.getOutputStream()));
            is=a_socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is));

            file_name1 = "#"+file_name+"\n";
            writer.write(file_name1);
            Log.d("SensorWrite",file_name1);
            writer.flush();
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

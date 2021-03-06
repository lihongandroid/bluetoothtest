package com.example.lihong.BluetoothTest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.lihong.myapplication.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothAdapter bTAdapter;
    private ListView listView;
    private BlueToothDeviceAdapter adapter;

    private TextView text_state;
    private TextView text_msg;

    private static final UUID BT_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final int BUFFER_SIZE=1024;
    private static final String  NAME="BT_DEMO";

    private ConnectThread connectThread;
    private ListenerThread listenerThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        bTAdapter=BluetoothAdapter.getDefaultAdapter();
        initReceiver();

        listenerThread=new ListenerThread();
        listenerThread.start();
    }

    private void initView(){
        findViewById(R.id.btn_openBT).setOnClickListener(this);
        findViewById(R.id.btn_search).setOnClickListener(this);
        findViewById(R.id.btn_send).setOnClickListener(this);
        text_state=(TextView)findViewById(R.id.text_state);
        text_msg=(TextView)findViewById(R.id.text_msg);

        listView=(ListView)findViewById(R.id.listView);
        adapter=new BlueToothDeviceAdapter(getApplicationContext(),R.layout.bluetooth_device_list_item);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent,View view,int position,long id){
                if(bTAdapter.isDiscovering()){
                    bTAdapter.cancelDiscovery();
                }
                BluetoothDevice device=(BluetoothDevice)adapter.getItem(position);
                //连接设备
                connectDevice(device);
            }
        });

    }

    private void initReceiver(){
        //注册广播
        IntentFilter filter=new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver,filter);
    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.btn_openBT:
                openBlueTooth();
                break;
            case R.id.btn_search:
                searchDevices();
                break;
            case R.id.btn_send:
                if(connectThread!=null){
                    connectThread.sendMsg("这是蓝牙发送过来的信息");
                }

        }
    }

    /*
    开启蓝牙
     */
    private void openBlueTooth(){
        if(bTAdapter==null){
            Toast.makeText(this,"当前设备不支持蓝牙功能",Toast.LENGTH_SHORT).show();
        }
        if(!bTAdapter.isEnabled()){
            /*
            Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);
             */
            bTAdapter.enable();
        }
        //开启被其他蓝牙发现的功能
        if(bTAdapter.getScanMode()!=BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //设置为一直开启
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,0);
            startActivity(i);
        }
    }
    /*
    搜索蓝牙设备
     */
    private void searchDevices(){
        if(bTAdapter.isDiscovering()){
            bTAdapter.cancelDiscovery();
        }
        getBoundedDevices();//获取已经配对的设备
        bTAdapter.startDiscovery();
       // initReceiver();
    }

    /*
    获取已经配对过的设备
     */
    private void getBoundedDevices(){
        //获取已经配对过的设备
        Set<BluetoothDevice> pairedDevices=bTAdapter.getBondedDevices();
        //将其添加到设备列表中
        if(pairedDevices.size()>0){
            for(BluetoothDevice device:pairedDevices){
                adapter.add(device);
            }
        }
    }

    /*
    连接蓝牙设备
     */
    private void connectDevice(BluetoothDevice device){
        text_state.setText(getResources().getString(R.string.connecting));

        try{
            //创建Socket
            BluetoothSocket socket=device.createRfcommSocketToServiceRecord(BT_UUID);//作为客户端主动连接
            //开启连接线程
            connectThread=new ConnectThread(socket,true);
            connectThread.start();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //取消搜索
        if(bTAdapter!=null&&bTAdapter.isDiscovering()){
            bTAdapter.cancelDiscovery();
        }
        //注销BroadcastReceiver,防止资源泄露
        unregisterReceiver(mReceiver);
    }

    private final BroadcastReceiver mReceiver=new BroadcastReceiver(){
        @Override
        public void onReceive(Context context,Intent intent){
            String action=intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //避免重复添加已经绑定过的设备
                if(device.getBondState()!=BluetoothDevice.BOND_BONDED){
                    adapter.add(device);
                    adapter.notifyDataSetChanged();
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Toast.makeText(MainActivity.this,"开始搜索",Toast.LENGTH_SHORT).show();
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Toast.makeText(MainActivity.this,"搜索完毕",Toast.LENGTH_SHORT).show();
            }
        }
    };

    /*
    连接线程
     */
    private class ConnectThread extends Thread{
        private BluetoothSocket socket;
        private boolean activeConnect;//是否主动连接：true为主动连接，false为被动连接
        InputStream inputStream;
        OutputStream outputStream;

        private ConnectThread(BluetoothSocket socket,boolean activeConnect){
            this.socket=socket;
            this.activeConnect=activeConnect;
        }

        @Override
        public void run(){
                    try {
                        //如果true,则为主动连接，直接调用连接方法
                        if (activeConnect) {
                            socket.connect();
                        }
                        text_state.post(new Runnable() {
                            @Override
                            public void run() {
                                text_state.setText(getResources().getString(R.string.connect_success));
                            }
                        });
                        inputStream = socket.getInputStream();
                        outputStream = socket.getOutputStream();

                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytes;
                        while (true) {
                            //读取数据
                            bytes = inputStream.read(buffer);
                            if (bytes > 0) {
                                final byte[] data = new byte[bytes];
                                System.arraycopy(buffer, 0, data, 0, bytes);
                                text_msg.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        text_msg.setText(getResources().getString(R.string.get_msg) + new String(data));
                                    }
                                });
                            }
                        }

                    } catch(IOException e){
                        e.printStackTrace();
                        text_state.post(new Runnable() {
                            @Override
                            public void run() {
                                text_state.setText(getResources().getString(R.string.connect_error));
                            }
                        });

                        try{
                            socket.close();
                        }catch (IOException e1){
                            e1.printStackTrace();
                        }
                    }
        }
        /*
   发送数据
    */
        public void sendMsg(final String msg){
            byte[] bytes=msg.getBytes();
            if(outputStream!=null){
                try{
                    //发送数据
                    outputStream.write(bytes);
                    text_msg.post(new Runnable() {
                        @Override
                        public void run() {
                            text_msg.setText(getResources().getString(R.string.send_msgs)+msg);
                        }
                    });
                }catch (IOException e){
                    e.printStackTrace();
                    text_msg.post(new Runnable() {
                        @Override
                        public void run() {
                            text_msg.setText(getResources().getString(R.string.send_msgs_error)+msg);
                        }
                    });
                }
            }
        }

    }


    /*
    监听线程，监听是否有别的设备请求连接我们的设备
     */
    private class ListenerThread extends Thread{
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;

        @Override
        public void run(){
            try{
                serverSocket=bTAdapter.listenUsingRfcommWithServiceRecord(NAME,BT_UUID);
                while(true){
                    //线程阻塞，等待别的设备来连接
                    socket=serverSocket.accept();
                    text_state.post(new Runnable() {
                        @Override
                        public void run() {
                            text_state.setText(getResources().getString(R.string.connecting));
                        }
                    });
                    connectThread=new ConnectThread(socket,false);
                    connectThread.start();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}


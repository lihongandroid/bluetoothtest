package com.example.lihong.BluetoothTest;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.example.lihong.myapplication.R;

/**
 * Created by lihong on 2017/10/10.
 */
public class BlueToothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {

    private final LayoutInflater mInflater;
    private int mResource;

    public BlueToothDeviceAdapter(Context context,int resource){
        super(context,resource);
        mInflater=LayoutInflater.from(context);
        mResource=resource;
    }
    @Override
    public View getView(int position,View converView,ViewGroup parent){
        if(converView==null){
            converView=mInflater.inflate(mResource,parent,false);
        }

        TextView name=(TextView)converView.findViewById(R.id.device_name);
        TextView info=(TextView)converView.findViewById(R.id.device_info);
        BluetoothDevice device=getItem(position);
        name.setText(device.getName());
        info.setText(device.getAddress());

        return converView;
    }
}

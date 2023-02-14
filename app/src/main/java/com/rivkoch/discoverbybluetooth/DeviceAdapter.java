package com.rivkoch.discoverbybluetooth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {

    private List<Device> deviceList;
    private Context context;

    public DeviceAdapter(Context context, List<Device> deviceList) {
        this.context=context;
        deviceList=new ArrayList<>();
        this.deviceList=deviceList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from (parent.getContext ());
        View viewItem = layoutInflater.inflate (R.layout.device_item, parent, false);
        return new MyViewHolder(viewItem);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Device device=deviceList.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void clearList(){
        int oldSize=getItemCount();
        this.deviceList.clear();
        this.notifyItemRangeRemoved(0,oldSize);
    }

    public void addToList( Device device){
        this.deviceList.add(device);
        this.notifyItemInserted(deviceList.size()-1);
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView deviceItem_TXT_deviceName, deviceItem_TXT_deviceDistance;
        public MyViewHolder(@NonNull View itemView) {

            super(itemView);
            deviceItem_TXT_deviceName = itemView.findViewById(R.id.deviceItem_TXT_deviceName);
            deviceItem_TXT_deviceDistance = itemView.findViewById(R.id.deviceItem_TXT_deviceDistance);
        }
        public void bind( Device device ){
            deviceItem_TXT_deviceName.setText(device.getName());
            deviceItem_TXT_deviceDistance.setText(device.getDistance() + "m from you.");
        }
    }

}
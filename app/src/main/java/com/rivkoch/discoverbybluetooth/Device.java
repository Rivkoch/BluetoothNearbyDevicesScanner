package com.rivkoch.discoverbybluetooth;

import java.util.UUID;

public class Device {
    private String name;
    private double distance;

    public Device(){

    }

    public String getName() {
        return this.name;
    }
    public double getDistance() {
        return this.distance;
    }

    public void setName(String name) {
        this.name=name;
    }
    public void setDistance(double distance) {
        this.distance=distance;
    }




}

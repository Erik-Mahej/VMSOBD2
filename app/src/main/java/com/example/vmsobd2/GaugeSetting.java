package com.example.vmsobd2;

public class GaugeSetting {
    String metricName;
    String unit;
    int maxSpeed;

    public GaugeSetting(String metricName, String unit, int maxSpeed) {
        this.metricName = metricName;
        this.unit = unit;
        this.maxSpeed = maxSpeed;
    }

    public String getUnit() {
        return unit;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }
}


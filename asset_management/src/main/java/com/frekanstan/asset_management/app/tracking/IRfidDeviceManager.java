package com.frekanstan.asset_management.app.tracking;

public interface IRfidDeviceManager {

    OperationType getMode();

    boolean connect(String name);

    boolean isDeviceOnline();

    void triggerButton();

    boolean isScanning();

    void scanBarcode();

    void setTargetTag(String targetTag);

    void writeTag(String targetTag, String newTag); //TODO:bool dönsün

    int getAntennaPower();

    void setAntennaPower(int percentage);

    void setLastAntennaPower(int percentage);

    int getBatteryPercentage();

    void onResume(OperationType mode);

    void onPause();

    void onDestroy();

    enum OperationType {
        Inventory,
        TagFinder,
        ReadWrite
    }
}
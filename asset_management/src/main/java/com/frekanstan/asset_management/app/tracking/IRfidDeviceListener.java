package com.frekanstan.asset_management.app.tracking;

public interface IRfidDeviceListener {
    enum DataType {
        RFID,
        Barcode,
        Proximity,
        Info,
        Trigger,
        Error
    }

    enum ConnectionState {
        Connecting,
        Connected,
        Disconnected
    }

    void ReceiveData(DataType type, String data);

    void onConnectionStateChanged(ConnectionState state, String connectedDeviceName);
}

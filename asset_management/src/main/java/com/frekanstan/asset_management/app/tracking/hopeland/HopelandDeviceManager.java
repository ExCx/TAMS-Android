package com.frekanstan.asset_management.app.tracking.hopeland;

import android.util.Log;

import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceListener;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.data.helpers.TryParsers;
import com.frekanstan.asset_management.view.MainActivityBase;
import com.rfidread.Enumeration.eReadType;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;
import com.rfidread.RFID_Option;
import com.rfidread.Tag6C;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

import static com.rfidread.RFIDReader.CreateBT4Conn;
import static com.rfidread.RFIDReader.GetBT4DeviceStrList;
import static com.rfidread.RFIDReader.GetBluetoothDeviceSOC;
import static com.rfidread.RFIDReader.OpenBluetooth;

public class HopelandDeviceManager implements IRfidDeviceManager {

    private final IAsynchronousMessage hopeListener;
    private final IRfidDeviceListener listener;
    @Getter
    private OperationType mode;
    private String name;
    private boolean isConnected, isScanning;
    @Setter
    private int lastAntennaPower = 100;

    public HopelandDeviceManager(MainActivityBase context, IRfidDeviceListener listener) {
        this.listener = listener;
        isConnected = false;
        isScanning = false;

        hopeListener = new IAsynchronousMessage() {
            @Override
            public void WriteDebugMsg(String s) {
                Log.d("hope_debug", s);
            }

            @Override
            public void WriteLog(String s) {
                Log.d("hope_log", s);
            }

            @Override
            public void PortConnecting(String s) {
                Log.d("hope_port_connecting", s);
            }

            @Override
            public void PortClosing(String s) {
                Log.d("hope_port_closing", s);
            }

            @Override
            public void OutPutTags(Tag_Model tag_model) {
                switch (mode) {
                    case Inventory:
                        listener.ReceiveData(IRfidDeviceListener.DataType.RFID, tag_model._EPC);
                        break;
                    case TagFinder:
                        if (!tag_model._EPC.equals(targetEpc))
                            return;
                        var rssi = tag_model._RSSI;
                        if (rssi > 100)
                            rssi = 100;
                        listener.ReceiveData(IRfidDeviceListener.DataType.Proximity, String.valueOf(rssi));
                        break;
                    case ReadWrite:
                        break;
                }
            }

            @Override
            public void OutPutTagsOver() {
                Log.d("hope_outputtagsover", "");
            }

            @Override
            public void GPIControlMsg(int gpiIndex, int state, int startOrStop) {
                Log.d("hope_gpi_msg", gpiIndex + ", " + state + ", " + startOrStop);
                if (gpiIndex == 0 && state == 0 && startOrStop == 0)
                    listener.ReceiveData(IRfidDeviceListener.DataType.Trigger, "ON");
                if (gpiIndex == 0 && state == 1 && startOrStop == 1)
                    listener.ReceiveData(IRfidDeviceListener.DataType.Trigger, "OFF");
                //triggerButton();
            }

            @Override
            public void OutPutScanData(byte[] bytes) {
                if (bytes.length > 5)
                    listener.ReceiveData(IRfidDeviceListener.DataType.Barcode, new String(Arrays.copyOfRange(bytes, 4, bytes.length), StandardCharsets.UTF_8));
                else
                    listener.ReceiveData(IRfidDeviceListener.DataType.Error, context.getString(R.string.invalid_qrcode_read));
            }
        };
    }

    @Override
    public boolean connect(String name) {
        this.name = name;
        OpenBluetooth();
        GetBT4DeviceStrList();
        if (CreateBT4Conn(name, hopeListener)) {
            listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Connected, name);
            RFIDReader._Config.SetEPCBaseBandParam(name, 255, 11, 1, 0);
            isConnected = true;
        }
        else {
            listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Disconnected, name);
            isConnected = false;
        }
        return isConnected;
    }

    @Override
    public boolean isDeviceOnline() {
        return isConnected;
    }

    @Override
    public void triggerButton() {
        if (mode == null)
            return;
        switch (mode) {
            case Inventory:
                if (isScanning)
                    RFIDReader._Config.Stop(name);
                else
                    Tag6C.GetEPC(name, 1, eReadType.Inventory);
                isScanning = !isScanning;
                break;
            case TagFinder:
                if (isScanning)
                    RFIDReader._Config.Stop(name);
                else
                    Tag6C.GetEPC_MatchEPC(name, 1, eReadType.Inventory, targetEpc);
                isScanning = !isScanning;
                break;
            case ReadWrite:
                break;
        }
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public void scanBarcode() { }

    private String targetEpc = "";
    @Override
    public void setTargetTag(String targetTag) {
        targetEpc = targetTag;
    }

    @Override
    public void writeTag(String targetTag, String newTag) {

    }

    @Override
    public int getAntennaPower() {
        val props = RFIDReader._Config.GetReaderProperty(name).split("\\|");
        if (TryParsers.tryParseInt(props[0]) == -1) {
            listener.ReceiveData(IRfidDeviceListener.DataType.Error, "Anten gücü tespit edilemedi");
            return 0;
        }
        val min = Float.parseFloat(props[0]);
        val max = Float.parseFloat(props[1]);
        val curr = Integer.parseInt(RFID_Option.GetANTPowerParam(name).split(",")[1]);
        return Math.round((curr - min) / (max - min) * 100F);
    }

    @Override
    public void setAntennaPower(int percentage) {
        val props = RFIDReader._Config.GetReaderProperty(name).split("\\|");
        if (TryParsers.tryParseInt(props[0]) == -1) {
            listener.ReceiveData(IRfidDeviceListener.DataType.Error, "Anten gücü ayarlanamadı");
            return;
        }
        val min = Float.parseFloat(props[0]);
        val max = Float.parseFloat(props[1]);
        val valueToSet = Math.round(min + ((max - min) / 100F * percentage));
        HashMap<Integer, Integer> dicPower = new HashMap<>();
        dicPower.put(1, valueToSet);
        if (RFIDReader._Config.SetANTPowerParam(name, dicPower) != 0)
            listener.ReceiveData(IRfidDeviceListener.DataType.Error, "Anten gücü ayarlanamadı");
        else
            listener.ReceiveData(IRfidDeviceListener.DataType.Info, "Anten gücü " + valueToSet + " olarak ayarlandı");
    }

    @Override
    public int getBatteryPercentage() {
        return Integer.parseInt(GetBluetoothDeviceSOC(name));
    }

    @Override
    public void onResume(OperationType mode) {
        this.mode = mode;
        switch (mode) {
            case Inventory:
                setAntennaPower(lastAntennaPower);
                break;
            case TagFinder:
                setAntennaPower(100);
                break;
            case ReadWrite:
                setAntennaPower(10);
                break;
        }
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }
}

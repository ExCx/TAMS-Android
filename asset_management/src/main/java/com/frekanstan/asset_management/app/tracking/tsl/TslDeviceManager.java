package com.frekanstan.asset_management.app.tracking.tsl;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceListener;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.data.helpers.TryParsers;
import com.uk.tsl.rfid.DeviceListActivity;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.device.ConnectionState;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager;
import com.uk.tsl.rfid.asciiprotocol.enumerations.Databank;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SwitchAction;
import com.uk.tsl.utils.HexEncoding;
import com.uk.tsl.utils.Observable;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.app.tracking.tsl.RfidDeviceModel.SINGLE_PRESS_SINGLE_DELAY_MS;
import static com.uk.tsl.rfid.DeviceListActivity.EXTRA_DEVICE_ACTION;
import static com.uk.tsl.rfid.DeviceListActivity.EXTRA_DEVICE_INDEX;

public class TslDeviceManager implements IRfidDeviceManager {
    private final Activity context;
    private final RfidDeviceModel model;
    private Reader reader, lastUserDisconnectedReader;
    private final IRfidDeviceListener listener;
    @Getter
    private OperationType mode;
    @Setter
    private int lastAntennaPower = 100;

    public TslDeviceManager(Activity context, IRfidDeviceListener listener) {
        this.context = context;
        this.listener = listener;

        AsciiCommander.createSharedInstance(context);
        final AsciiCommander commander = getCommander();
        commander.clearResponders();
        //commander.addResponder(new LoggerResponder());
        commander.addSynchronousResponder();

        ReaderManager.create(context);
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);

        model = new RfidDeviceModel();
        model.setCommander(commander);
        model.setHandler(new GenericHandler(this));

        val deviceConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                val connectionStateMsg = getCommander().getConnectionState().toString();
                Log.d("", "AsciiCommander state changed - isConnected: " + getCommander().isConnected() + " (" + connectionStateMsg + ")");
                Log.d("TFA", String.format("IsConnecting: %s", reader == null ? "No Reader" : reader.isConnecting()));
                if (getCommander() != null) {
                    switch (getCommander().getConnectionState()) {
                        case CONNECTED:
                            listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Connected, getCommander().getConnectedDeviceName());
                            model.setSwitchBehaviour(SwitchAction.OFF, SwitchAction.OFF, SINGLE_PRESS_SINGLE_DELAY_MS);
                            break;
                        case CONNECTING:
                            listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Connecting, "");
                            break;
                        case DISCONNECTED:
                            listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Disconnected, "");
                            if (reader != null && !reader.wasLastConnectSuccessful())
                                reader = null;
                            break;
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(deviceConnectionReceiver,
                new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));
    }

    private static class GenericHandler extends WeakHandler<TslDeviceManager> {
        GenericHandler(TslDeviceManager t) {
            super(t);
        }

        @Override
        public void handleMessage(Message msg, TslDeviceManager t) {
            switch (msg.what) {
                case ModelBase.BUSY_STATE_CHANGED_NOTIFICATION:
                    if (t.model.error() != null)
                        t.listener.ReceiveData(IRfidDeviceListener.DataType.Error, "\n Task failed:\n" + t.model.error().getMessage() + "\n\n");
                    break;
                case ModelBase.MESSAGE_NOTIFICATION:
                    String message = (String) msg.obj;
                    if (message.startsWith("EPC:")) {
                        var splitted = message.split("\n");
                        for (val line : splitted) {
                            Log.d("rfid_rfid", line);
                            if (line.startsWith("EPC:")) {
                                t.listener.ReceiveData(IRfidDeviceListener.DataType.RFID, line.substring(5));
                                break;
                            }
                        }
                    }
                    else if (message.startsWith("BC:"))
                        t.listener.ReceiveData(IRfidDeviceListener.DataType.Barcode, message.substring(4));
                    else if (message.startsWith("GPI:"))
                        t.listener.ReceiveData(IRfidDeviceListener.DataType.Trigger, message.substring(5));
                    else if (message.startsWith("RNG:")) {
                        var proximityPercentage = TryParsers.tryParseInt(message.substring(5));
                        if (proximityPercentage != -1)
                            t.listener.ReceiveData(IRfidDeviceListener.DataType.Proximity, String.valueOf(proximityPercentage));
                    }
                    else if (message.startsWith("ER:")) {
                        if (t.model.error() != null)
                            t.listener.ReceiveData(IRfidDeviceListener.DataType.Error, "\n Task failed:\n" + t.model.error().getMessage() + "\n\n");
                        else
                            t.listener.ReceiveData(IRfidDeviceListener.DataType.Error, message);
                    }
                    else
                        t.listener.ReceiveData(IRfidDeviceListener.DataType.Info, message);
                    break;
            }
        }
    }

    @Override
    public boolean connect(String name) {
        ReaderManager.sharedInstance().onResume();
        ReaderManager.sharedInstance().updateList();
        try {
            val serial = name.split("-")[2] + "-" + name.split("-")[1] + "-" + name.split("-")[0];
            val list = ReaderManager.sharedInstance().getReaderList().list();
            for (val device : list) {
                if (serial.equals(device.getSerialNumber())) {
                    reader = device;
                    lastUserDisconnectedReader = null;
                    getCommander().setReader(reader);
                    autoSelectReader(!ReaderManager.sharedInstance().didCauseOnPause());
                    return true;
                }
            }
            showDeviceList();
            return false;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private void showDeviceList() {
        int index = -1;
        if (reader != null)
            index = ReaderManager.sharedInstance().getReaderList().list().indexOf(reader);
        val selectIntent = new Intent(context, DeviceListActivity.class);
        if (index >= 0)
            selectIntent.putExtra(EXTRA_DEVICE_INDEX, index);
        context.startActivityForResult(selectIntent, DeviceListActivity.SELECT_DEVICE_REQUEST);
    }

    public void onDeviceSelected(Intent data) {
        if (data.getExtras() != null) {
            int readerIndex = data.getExtras().getInt(EXTRA_DEVICE_INDEX);
            val chosenReader = ReaderManager.sharedInstance().getReaderList().list().get(readerIndex);
            int action = data.getExtras().getInt(EXTRA_DEVICE_ACTION);
            if (action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_CONNECT) { // Use the Reader found
                reader = chosenReader;
                lastUserDisconnectedReader = null;
                getCommander().setReader(reader);
                autoSelectReader(!ReaderManager.sharedInstance().didCauseOnPause());
            }
        }
    }

    @Override
    public boolean isDeviceOnline() {
        return reader != null && getCommander().isConnected();
    }

    @Override
    public void triggerButton() {
        if (mode == null)
            return;
        model.scan();
    }

    @Override
    public boolean isScanning() { return model.isScanning(); }

    @Override
    public void scanBarcode() {
        model.scanBarcode();
    }

    @Override
    public void setTargetTag(String targetTag) {
        model.setTargetTagEpc(targetTag);
    }

    //READ WRITE
    @Override
    public void writeTag(String targetTag, String newTag) {
        model.getWriteCommand().setOffset(2);
        model.getWriteCommand().setLength(6);
        model.getWriteCommand().setBank(Databank.ELECTRONIC_PRODUCT_CODE);
        model.getWriteCommand().setSelectData(targetTag);
        try {
            model.getWriteCommand().setData(HexEncoding.stringToBytes(newTag));
        } catch (Exception e) {
            // Ignore if invalid
        }
        model.write();
    }

    @Override
    public int getAntennaPower() {
        return model.getDevicePowerPercentage();
    }

    @Override
    public void setAntennaPower(int percentage) {
        model.setDevicePowerPercentage(percentage);
    }

    @Override
    public int getBatteryPercentage() {
        return model.getBatteryLevel();
    }

    //----------------------------------------------------------------------------------------------
    // Events
    //----------------------------------------------------------------------------------------------

    @Override
    public synchronized void onResume(OperationType mode) {
        this.mode = mode;
        ReaderManager.sharedInstance().onResume();
        ReaderManager.sharedInstance().updateList();
        autoSelectReader(!ReaderManager.sharedInstance().didCauseOnPause());
        if (!getCommander().isConnected())
            Toast.makeText(context, R.string.device_is_not_connected, Toast.LENGTH_LONG).show();
        else
            model.enableMode(mode);

    }

    @Override
    public synchronized void onPause() {
        getCommander().clearResponders();
        //getCommander().addResponder(new LoggerResponder());
        getCommander().addSynchronousResponder();
        ReaderManager.sharedInstance().onPause();
    }

    @Override
    public synchronized void onDestroy() {
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().removeObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().removeObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().removeObserver(mRemovedObserver);
    }

    //----------------------------------------------------------------------------------------------
    // Observers
    //----------------------------------------------------------------------------------------------

    private final Observable.Observer<Reader> mAddedObserver = (observable, reader) ->
            autoSelectReader(true);

    private final Observable.Observer<Reader> mUpdatedObserver = (observable, reader) -> {
        if (reader == lastUserDisconnectedReader)
            lastUserDisconnectedReader = null;
        if (reader == this.reader && !reader.isConnected()) {
            this.reader = null;
            getCommander().setReader(reader);
        } else
            autoSelectReader(true);
    };

    private final Observable.Observer<Reader> mRemovedObserver = (observable, reader) -> {
        if (reader == lastUserDisconnectedReader)
            lastUserDisconnectedReader = null;
        if (reader == this.reader) {
            this.reader = null;
            getCommander().setReader(null);
        }
    };

    //----------------------------------------------------------------------------------------------
    // Misc
    //----------------------------------------------------------------------------------------------

    private void autoSelectReader(boolean attemptReconnect) {
        if (reader != null
                && !reader.isConnecting()
                && (reader.getActiveTransport() == null || reader.getActiveTransport().connectionStatus().value() == ConnectionState.DISCONNECTED)) {
            if (attemptReconnect) {
                if (reader.allowMultipleTransports() || reader.getLastTransportType() == null) {
                    if (reader.connect())
                        listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Connecting, reader.getDisplayName());
                    else
                        listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Disconnected, reader.getDisplayName());
                } else {
                    if (reader.connect(reader.getLastTransportType()))
                        listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Connecting, reader.getDisplayName());
                    else
                        listener.onConnectionStateChanged(IRfidDeviceListener.ConnectionState.Disconnected, reader.getDisplayName());
                }
            }
        }
    }

    private AsciiCommander getCommander() {
        return AsciiCommander.sharedInstance();
    }
}
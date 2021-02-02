package com.frekanstan.asset_management.app.tracking.nur;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.frekanstan.asset_management.R;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceListener;
import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.frekanstan.asset_management.view.MainActivityBase;
import com.nordicid.nurapi.ACC_IMAGER_TYPE;
import com.nordicid.nurapi.AccBarcodeResult;
import com.nordicid.nurapi.AccBarcodeResultListener;
import com.nordicid.nurapi.AccessoryExtension;
import com.nordicid.nurapi.AntennaMapping;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiAutoConnectTransport;
import com.nordicid.nurapi.NurApiErrors;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurApiSocketAutoConnect;
import com.nordicid.nurapi.NurEventAutotune;
import com.nordicid.nurapi.NurEventClientInfo;
import com.nordicid.nurapi.NurEventDeviceInfo;
import com.nordicid.nurapi.NurEventEpcEnum;
import com.nordicid.nurapi.NurEventFrequencyHop;
import com.nordicid.nurapi.NurEventIOChange;
import com.nordicid.nurapi.NurEventInventory;
import com.nordicid.nurapi.NurEventNxpAlarm;
import com.nordicid.nurapi.NurEventProgrammingProgress;
import com.nordicid.nurapi.NurEventTagTrackingChange;
import com.nordicid.nurapi.NurEventTagTrackingData;
import com.nordicid.nurapi.NurEventTraceTag;
import com.nordicid.nurapi.NurEventTriggeredRead;
import com.nordicid.nurapi.NurRespInventory;
import com.nordicid.nurapi.NurTag;
import com.nordicid.nurapi.NurTagStorage;
import com.nordicid.tdt.EPCTagEngine;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

import static com.nordicid.nurapi.NurApi.SETUP_PERANTPOWER;

public class NurDeviceManager implements IRfidDeviceManager {

    private final MainActivityBase context;
    @Getter @Setter
    private OperationType mode;
    private final NurApi mNurApi;
    private final NurApiAutoConnectTransport mAuto;
    boolean mTriggerDown;
    IRfidDeviceListener listener;
    private static AccessoryExtension mAccExt;
    private boolean isScanning, isAiming, isScanningBarcode;
    @Setter
    private int lastAntennaPower = 100;

    //This counter add by one when single tag found after inventory. Reset to zero if multiple tags found.
    int mSingleTagFoundCount;

    //This is true while searching single tag operation ongoing.
    boolean mSingleTagDoTask;

    //This counts scan rounds and when reaching 15 it's time to stop.
    int mSingleTagRoundCount;

    //Temporary storing current TX level because single tag will be search using low TX level
    int mSingleTempTxLevel;

    //This variable hold last tag epc for making sure same tag found 3 times in row.
    static String mTagUnderReview;

    private final TraceTagController mTraceController;

    public NurDeviceManager(MainActivityBase context, IRfidDeviceListener listener) {
        this.context = context;
        this.listener = listener;
        mNurApi = new NurApi();
        mNurApi.setListener(mNurApiListener);

        mAuto = new NurApiSocketAutoConnect(this.context, mNurApi);
        mAuto.setAddress("127.0.0.1:6734/?name=HH53");

        mTriggerDown = false;
        mSingleTagDoTask = false;
        isScanning = false;
        isScanningBarcode = false;
        isAiming = false;
        mTagUnderReview = "";

        mAccExt = new AccessoryExtension(mNurApi);
        mAccExt.registerBarcodeResultListener(mBarcodeResult);

        mTraceController = new TraceTagController(mNurApi);
        mTraceController.setListener(new TraceTagController.TraceTagListener() {
            @Override
            public void traceTagEvent(TraceTagController.TracedTagInfo data) {
                listener.ReceiveData(IRfidDeviceListener.DataType.Proximity, String.valueOf(data.scaledRssi));
            }

            @Override
            public void readerDisconnected() {
                if (isScanning)
                    traceRfid();
            }

            @Override
            public void readerConnected() { }

            @Override
            public void IOChangeEvent(NurEventIOChange event) { }

        });
    }

    private final NurApiListener mNurApiListener = new NurApiListener()
    {
        @Override
        public void triggeredReadEvent(NurEventTriggeredRead event) { Log.d("NUR", "triggeredReadEvent"); }
        @Override
        public void traceTagEvent(NurEventTraceTag event) {Log.d("NUR", "traceTagEvent"); }
        @Override
        public void programmingProgressEvent(NurEventProgrammingProgress event) {Log.d("NUR", "programmingProgressEvent"); }
        @Override
        public void nxpEasAlarmEvent(NurEventNxpAlarm event) {Log.d("NUR", "nxpEasAlarmEvent"); }
        @Override
        public void logEvent(int level, String txt) { Log.d("NURLOG", txt);}
        @Override
        public void inventoryStreamEvent(NurEventInventory event) {
            try {
                if (event.tagsAdded > 0) {
                    NurTagStorage tagStorage = mNurApi.getStorage(); //Storage contains all tags found
                    //Iterate just received tags based on event.tagsAdded
                    for (int x = 0; x < event.tagsAdded; x++) {
                        String epcString;
                        NurTag tag = tagStorage.get(x);
                        epcString = NurApi.byteArrayToHexString(tag.getEpc());
                        listener.ReceiveData(IRfidDeviceListener.DataType.RFID, epcString);
                    }
                }
            }
            catch (Exception ex)
            {
                Log.d("NURERROR", Objects.requireNonNull(ex.getMessage()));
            }
        }

        @Override
        public void inventoryExtendedStreamEvent(NurEventInventory event) {Log.d("NUR", "inventoryExtendedStreamEvent");}
        @Override
        public void frequencyHopEvent(NurEventFrequencyHop event) {Log.d("NUR", "frequencyHopEvent"); }
        @Override
        public void epcEnumEvent(NurEventEpcEnum event) {Log.d("NUR", "epcEnumEvent"); }
        @Override
        public void disconnectedEvent() { Log.d("NUR", "disconnectedEvent");}
        @Override
        public void deviceSearchEvent(NurEventDeviceInfo event) {Log.d("NUR", "deviceSearchEvent"); }
        @Override
        public void debugMessageEvent(String event) {Log.d("NUR", "debugMessageEvent"); }

        @Override
        public void connectedEvent() {
            Log.d("NUR", "connectedEvent");
            try {
                mAccExt.imagerCmd("@MENU_OPTO@ZZ@A0@A00@A0W@A0Y@A10@A14@Q0@Q0@A1C@Q0@Q0@A1L@A18@A1F@A0u@ZZ@OTPO_UNEM@", ACC_IMAGER_TYPE.Opticon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void clientDisconnectedEvent(NurEventClientInfo event) {Log.d("NUR", "clientDisconnectedEvent"); }
        @Override
        public void clientConnectedEvent(NurEventClientInfo event) {Log.d("NUR", "clientConnectedEvent"); }
        @Override
        public void bootEvent(String event) {Log.d("NUR", "bootEvent");}

        @Override
        public void IOChangeEvent(NurEventIOChange event) {
            Log.d("NUR", "IOChangeEvent");
            if (event.source == 100) {
                triggerButton();
                if (event.direction == 1)
                    listener.ReceiveData(IRfidDeviceListener.DataType.Trigger, "ON");
                else if (event.direction == 0)
                    listener.ReceiveData(IRfidDeviceListener.DataType.Trigger, "OFF");
            }
        }

        @Override
        public void autotuneEvent(NurEventAutotune event) {Log.d("NUR", "autotuneEvent"); }
        @Override
        public void tagTrackingScanEvent(NurEventTagTrackingData event) {Log.d("NUR", "tagTrackingScanEvent"); }
        @Override
        public void tagTrackingChangeEvent(NurEventTagTrackingChange event) {Log.d("NUR", "tagTrackingChangeEvent"); }
    };

    @Override
    public boolean connect(String name) {
        return true;
    }

    @Override
    public boolean isDeviceOnline() {
        return mNurApi.isConnected();
    }

    @Override
    public void triggerButton() {
        if (mode == null)
            return;
        switch (mode) {
            case Inventory:
                scanRfid();
                break;
            case ReadWrite:
                scanSingleTagThread();
                break;
            case TagFinder:
                traceRfid();
                break;
        }
    }

    private void scanRfid()
    {
        if (isScanning) //stop scan
        {
            try {
                if (mNurApi.isInventoryStreamRunning())
                    mNurApi.stopInventoryStream();
            }
            catch (Exception ex)
            {
                Log.d("NURERROR", Objects.requireNonNull(ex.getMessage()));
            }
        }
        else {
            try {
                mNurApi.clearIdBuffer(); //This command clears all tag data currently stored into the module’s memory as well as the API's internal storage.
                mNurApi.startInventoryStream(); //Kick inventory stream on. Now inventoryStreamEvent handler offers inventory results.
            } catch (Exception ex) {
                Log.d("NURERROR", Objects.requireNonNull(ex.getMessage()));
            }
        }
        isScanning = !isScanning;
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public void scanBarcode()
    {
        if (isAiming) {
            try { mAccExt.imagerAIM(false); }
            catch (Exception ex) { Log.d("NURERROR", Objects.requireNonNull(ex.getMessage())); }
            isAiming = false;
            return;
        }
        if (isScanningBarcode) {
            try { mAccExt.cancelBarcodeAsync(); }
            catch (Exception ex) { Log.d("NURERROR", Objects.requireNonNull(ex.getMessage())); }
            isScanningBarcode = false;
            return;
        }
        //aiming
        isAiming = true;
        try { mAccExt.imagerAIM(true); }
        catch (Exception ex) { Log.d("NURERROR", Objects.requireNonNull(ex.getMessage())); }
        //scanning
        new Handler().postDelayed(() -> {
            isAiming = false;
            isScanningBarcode = true;
            try {
                mAccExt.imagerAIM(false);
                mAccExt.readBarcodeAsync(3000); //3 sec timeout
            }
            catch (Exception ex) { Log.d("NURERROR", Objects.requireNonNull(ex.getMessage())); }
            new Handler().postDelayed(() -> isScanningBarcode = false, 3000);
        }, 1500);
    }

    private AccBarcodeResultListener mBarcodeResult = new AccBarcodeResultListener() {
        @Override
        public void onBarcodeResult(AccBarcodeResult accBarcodeResult) {
            if (accBarcodeResult.status == NurApiErrors.NO_TAG) {
                Log.d("NURMSG", "No barcode found");
                isScanningBarcode = false;
            }
            else if (accBarcodeResult.status == NurApiErrors.NOT_READY)
                Log.d("NURMSG", "Cancelled");
            else if (accBarcodeResult.status != NurApiErrors.NUR_SUCCESS) {
                Log.d("NURERROR", "ErrorCode: " + accBarcodeResult.status);
                isScanningBarcode = false;
            }
            else {
                Log.d("NURBRCD", accBarcodeResult.strBarcode);
                listener.ReceiveData(IRfidDeviceListener.DataType.Barcode, accBarcodeResult.strBarcode);
                //context.runOnUiThread(() -> context.onBarcodeScanned(accBarcodeResult.strBarcode));
                try {
                    mAccExt.beepAsync(100);
                    if (mAccExt.getConfig().hasVibrator())
                        mAccExt.vibrate(200);
                }
                catch (Exception ex)
                {
                    Log.d("NURERROR", Objects.requireNonNull(ex.getMessage()));
                }
                isScanningBarcode = false;
            }
        }
    };

    private String targetEpc = "";

    @Override
    public void setTargetTag(String targetTag) {
        targetEpc = targetTag;
    }

    private void traceRfid() {
        if (isScanning) {
            context.onRangeScanned(0);
            if (mTraceController.isTracingTag())
                mTraceController.stopTagTrace();
        }
        else {
            try {
                if (!mTraceController.isTracingTag())
                    mTraceController.startTagTrace(targetEpc);
            }
            catch (Exception ex) {
                Toast.makeText(context, "Reader error", Toast.LENGTH_SHORT).show();
            }
        }
        isScanning = !isScanning;
    }

    @Override
    public int getAntennaPower() {
        try {
            val x = mNurApi.getModuleSetup(SETUP_PERANTPOWER);
            var antennaPowers = x.antPower;
            val powerOffset = mNurApi.getPowerOffset();
            antennaPowers = mNurApi.getPerAntennaPower();
            val f = mNurApi.getSetupTxLevel();
            return f + powerOffset + antennaPowers[0];
        } catch (Exception exception) {
            exception.printStackTrace();
            return 100;
        }
    }

    @Override
    public void setAntennaPower(int percentage) {

    }

    @Override
    public int getBatteryPercentage() {
        return 0;
    }

    //READWRITE

    @Override
    public void writeTag(String targetTag, String newTag)
    {
        new WriteOperation().execute(newTag, targetTag);
    }

    private class WriteOperation extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... params) {
            String writeResult; //If write success this remain empty string. otherwise error message
            try {
                byte[] newEpc = NurApi.hexStringToByteArray(params[0]);
                byte[] currentEpcBytes = NurApi.hexStringToByteArray(params[1]);
                writeResult = writeTagByEpc(currentEpcBytes, currentEpcBytes.length, newEpc.length, newEpc);
            }
            catch (Exception e)
            {
                writeResult = e.getMessage();
            }
            return writeResult; //result to onPostExecute
        }

        @Override
        protected void onPostExecute(String result) {
            if (!result.isEmpty()) {
                Log.d("NURERROR", result);
                Toast.makeText(context, R.string.transfer_unsuccessful_try_again, Toast.LENGTH_LONG).show();
            }
            /*else
                soundGenerator.success();*/
        }
    }

    /**
     * Perform Write operation.
     * Current EPC used for singulating correct tag.
     * When found, new EPC will be written.
     * return empty string if success. Otherwise error message.
     */
    private String writeTagByEpc(byte[] epcBuffer, int epcBufferLength, int newEpcBufferLength, byte[] newEpcBuffer) {

        String ret = "";
        int savedTxLevel = 0;
        int savedAntMask = 0;

        try {
            // Make sure antenna autoswitch is enabled
            if (mNurApi.getSetupSelectedAntenna() != NurApi.ANTENNAID_AUTOSELECT)
                mNurApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);

            //Let's use more TX power for write operation. Just making sure operation succeed.
            //Save current Tx level and Antenna settings
            savedTxLevel = mNurApi.getSetupTxLevel();
            savedAntMask = mNurApi.getSetupAntennaMaskEx();

            // Attempt to use circular antenna
            AntennaMapping[]map = mNurApi.getAntennaMapping();
            String ant="Circular";
            int circMask = 0;
            for (AntennaMapping antennaMapping : map) {
                if (antennaMapping.name.startsWith(ant))
                    circMask |= (1 << antennaMapping.antennaId);
            }

            if (circMask != 0)
                mNurApi.setSetupAntennaMaskEx(circMask); //Found from this dev so let's use Circular

            //Set full TX power
            mNurApi.setSetupTxLevel(0);

        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            ret = e.getMessage();
        }

        if (Objects.requireNonNull(ret).isEmpty()) {
            try {
                mNurApi.writeEpcByEpc(epcBuffer, epcBufferLength, newEpcBufferLength, newEpcBuffer);
            }
            catch (Exception e) {
                ret = e.getMessage();
            }
        }

        //Restore settings
        try {
            mNurApi.setSetupTxLevel(savedTxLevel);
            mNurApi.setSetupAntennaMaskEx(savedAntMask);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void scanSingleTagThread()
    {
        if (mSingleTagDoTask || mTriggerDown)
            return; //Already running tasks so let's not disturb that operation.
        context.runOnUiThread(() -> Toast.makeText(context, R.string.transferring_code, Toast.LENGTH_SHORT).show());

        Thread sstThread = new Thread(() -> {
            try {
                mSingleTempTxLevel = mNurApi.getSetupTxLevel(); //Store current TX level of RFID reader
                //Set rather low TX power. You need to get close to tag for successful reading
                mNurApi.setSetupTxLevel(NurApi.TXLEVEL_9); //This is attenuation level as dBm from max level 27dBm
            }
            catch (Exception ex) {
                Log.d("NURERROR", Objects.requireNonNull(ex.getMessage()));
                return;
            }

            mSingleTagDoTask = true;
            mSingleTagRoundCount = 0;
            mSingleTagFoundCount = 0;

            long time_start = System.currentTimeMillis();

            while (mSingleTagDoTask)
            {
                Log.d("NURMSG", "Scan single tag (round:" + mSingleTagRoundCount + ")");
                try {
                    mNurApi.clearIdBuffer(); //Clear buffer from existing tags
                    //Do the inventory with small rounds and Q values. We looking for single tag..
                    NurRespInventory resp = mNurApi.inventory(2, 4, 0); //Rounds=2, Q=4, Session=0

                    mSingleTagRoundCount++;
                    if (resp.numTagsFound > 1) {
                        Log.d("NURMSG", "Too many tags seen");
                        mSingleTagFoundCount = 0;
                    } else if (resp.numTagsFound == 1) {
                        NurTag tag = mNurApi.fetchTagAt(true, 0); //Get tag information from pos 0

                        //We looking for same tag in antenna field seen 3 times in row. isSameTag function make sure it is.
                        if (isSameTag(tag.getEpcString()))
                            mSingleTagFoundCount++;
                        else
                            mSingleTagFoundCount = 1; //It was not. Start all over.
                        if (mSingleTagFoundCount == 3) { //Single tag found multiple times (3) in row so let's accept.
                            try {
                                //Check if tag is GS1 coded. Exception fired if not and plain EPC shown.
                                //This is TDT (TagDataTranslation) library feature.
                                EPCTagEngine engine = new EPCTagEngine(tag.getEpcString());
                                //Looks like it is GS1 coded, show pure Identity URI
                                String gs = engine.buildPureIdentityURI();
                                Log.d("GS1 coded tag!", gs);
                            } catch (Exception ex) {
                                //Not GS1 coded, show EPC only
                                Log.d("Single Tag found!", "EPC=" + tag.getEpcString());
                                listener.ReceiveData(IRfidDeviceListener.DataType.RFID, tag.getEpcString());
                                //mContext.runOnUiThread(() -> mContext.onRfidScanned(tag.getEpcString()));
                            }
                            mSingleTagDoTask = false;
                        }
                    }

                    //timeout after 5 sec
                    if (System.currentTimeMillis() >= time_start + 5000)
                        mSingleTagDoTask = false;
                } catch (Exception ex)
                {Log.d("NURERROR", Objects.requireNonNull(ex.getMessage()));}
            }

            //Original TX level back
            try {
                mNurApi.setSetupTxLevel(mSingleTempTxLevel);
            }
            catch (Exception ex)
            {Log.d("NURERROR", Objects.requireNonNull(ex.getMessage()));}
        });

        sstThread.start();
    }

    @Override
    public void onResume(OperationType mode) {
        setMode(mode);
        mAuto.onResume();
    }

    public void onPause() {
        mAuto.onPause();
    }

    @Override
    public void onDestroy() {
        try {
            if (isScanning) {
                isScanning = false;
                mNurApi.stopInventoryStream();
            }
            if (isScanningBarcode) { //There is mScanning ongoing so we need just abort it
                isScanningBarcode = false;
                mAccExt.cancelBarcodeAsync();
            }
        }
        catch (Exception ex) {
            Log.i("NURERROR", "Exception barcode onStop:" + ex.getMessage());
        }
    }

    static boolean isSameTag(String epc)
    {
        if(epc.compareTo(mTagUnderReview) == 0)
            return true;
        else //set new
            mTagUnderReview = epc;
        return false;
    }
}

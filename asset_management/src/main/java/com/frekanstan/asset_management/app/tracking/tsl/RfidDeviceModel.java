package com.frekanstan.asset_management.app.tracking.tsl;

import com.frekanstan.asset_management.app.tracking.IRfidDeviceManager;
import com.uk.tsl.rfid.asciiprotocol.commands.BarcodeCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.InventoryCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.SwitchActionCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.WriteTransponderCommand;
import com.uk.tsl.rfid.asciiprotocol.enumerations.Databank;
import com.uk.tsl.rfid.asciiprotocol.enumerations.DuplicateRemovalMode;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySelect;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySession;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QueryTarget;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SelectAction;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SelectTarget;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SwitchAction;
import com.uk.tsl.rfid.asciiprotocol.enumerations.SwitchState;
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import com.uk.tsl.rfid.asciiprotocol.responders.AsciiSelfResponderCommandBase;
import com.uk.tsl.rfid.asciiprotocol.responders.BatteryStatusResponder;
import com.uk.tsl.rfid.asciiprotocol.responders.ICommandResponseLifecycleDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.SignalStrengthResponder;
import com.uk.tsl.rfid.asciiprotocol.responders.SwitchResponder;
import com.uk.tsl.utils.HexEncoding;
import com.uk.tsl.utils.StringHelper;

import java.util.Locale;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

class RfidDeviceModel extends ModelBase {
    //constants
    private final static int SINGLE_PRESS_REPEAT_DELAY_MS = 300;
    final static int SINGLE_PRESS_SINGLE_DELAY_MS = 750;

    //commands
    @Getter
    private final InventoryCommand inventoryCommand;
    @Getter
    private WriteTransponderCommand writeCommand;

    //responders
    private final InventoryCommand inventoryResponder;
    private final InventoryCommand singleReadResponder;
    private final BarcodeCommand barcodeResponder;
    private final BarcodeCommand barcodeCommand;
    private SignalStrengthResponder signalStrengthResponder;
    private final SwitchResponder switchResponder;
    private BatteryStatusResponder batteryStatusResponder;

    private boolean mAnyTagSeen;

    @Getter @Setter
    private boolean isScanning = false;

    @Getter
    private String targetTagEpc = null;

    RfidDeviceModel() {

        //inventory command
        inventoryCommand = new InventoryCommand();
        inventoryCommand.setResetParameters(TriState.YES);
        inventoryCommand.setIncludeChecksum(TriState.YES);
        inventoryCommand.setIncludePC(TriState.YES);

        //inventory responder
        inventoryResponder = new InventoryCommand();
        inventoryResponder.setCaptureNonLibraryResponses(true);
        inventoryResponder.setResponseLifecycleDelegate( new ICommandResponseLifecycleDelegate() { //responder lifecycle
            @Override
            public void responseBegan() {
                mAnyTagSeen = false;
            }
            @Override
            public void responseEnded() {
                if (!mAnyTagSeen && inventoryCommand.getTakeNoAction() != TriState.YES)
                    sendMessageNotification("No transponders seen");
                inventoryCommand.setTakeNoAction(TriState.NO);
            }
        });
        inventoryResponder.setTransponderReceivedDelegate((transponder, moreAvailable) -> { //transponder found
            mAnyTagSeen = true;
            String tidMessage = transponder.getTidData() == null ? "" : HexEncoding.bytesToString(transponder.getTidData());
            String infoMsg = "\nPC: " + transponder.getPc() + " CRC: " + transponder.getCrc();
            sendMessageNotification("EPC: " + transponder.getEpc() + infoMsg + "\nTID: " + tidMessage);
        });

        //single read responder
        singleReadResponder = new InventoryCommand();
        singleReadResponder.setCaptureNonLibraryResponses(true);
        singleReadResponder.setTransponderReceivedDelegate((transponder, moreAvailable) -> { //transponder found
            getCommander().removeResponder(singleReadResponder);
            sendMessageNotification("EPC: " + transponder.getEpc());
        });

        barcodeCommand = new BarcodeCommand();

        //barcode responder
        barcodeResponder = new BarcodeCommand();
        barcodeResponder.setCaptureNonLibraryResponses(true);
        barcodeResponder.setUseEscapeCharacter(TriState.YES);
        barcodeResponder.setBarcodeReceivedDelegate(barcode -> //barcode scanned
                sendMessageNotification("BC: " + barcode));

        //switch responder
        switchResponder = new SwitchResponder();
        switchResponder.setSwitchStateReceivedDelegate(switchState -> {
            if (switchState == SwitchState.OFF) //device switch released
            {
                setScanning(false);
                sendMessageNotification("GPI: OFF");
                // Fake a signal report for both percentage and RSSI to indicate action stopped
                if (signalStrengthResponder.getRawSignalStrengthReceivedDelegate() != null)
                    signalStrengthResponder.getRawSignalStrengthReceivedDelegate().signalStrengthReceived(null);
            }
            else if (switchState == SwitchState.SINGLE) { //device switch pressed
                setScanning(true);
                sendMessageNotification("GPI: ON");
            }
        });

        //signal strength responder
        signalStrengthResponder = new SignalStrengthResponder();
        signalStrengthResponder.setRawSignalStrengthReceivedDelegate(level -> { //raw signal received
            val value = level == null ? "0" : String.valueOf(convertSignalToPercentage(level));
            sendMessageNotification("RNG: " + (isScanning() ? value : "0"));
        });

        batteryStatusResponder = new BatteryStatusResponder();

        //write command
        writeCommand = WriteTransponderCommand.synchronousCommand();
    }

    void enableMode(IRfidDeviceManager.OperationType mode) {
        getCommander().addResponder(batteryStatusResponder);
        switch (mode) {
            case Inventory:
                getCommander().addResponder(inventoryResponder);
                getCommander().addResponder(barcodeResponder);
                getCommander().addResponder(switchResponder);
                break;
            case TagFinder:
                getCommander().addResponder(signalStrengthResponder);
                getCommander().addResponder(switchResponder);
                break;
            case ReadWrite:
                getCommander().addResponder(singleReadResponder);
                getCommander().addResponder(barcodeResponder);
                break;
        }
        try {
            performTask(() -> {
                resetDevice();
                switch (mode) {
                    case Inventory:
                        setSwitchBehaviour(SwitchAction.INVENTORY, SwitchAction.BARCODE, SINGLE_PRESS_REPEAT_DELAY_MS);
                        setDevicePowerPercentage(100);
                        getInventoryCommand().setQuerySession(QuerySession.SESSION_1);
                        getInventoryCommand().setQValue(11);
                        getInventoryCommand().setDuplicateRemoval(DuplicateRemovalMode.ON);
                        getInventoryCommand().setTakeNoAction(TriState.YES);
                        getCommander().executeCommand(getInventoryCommand());
                        break;
                    case TagFinder:
                        setSwitchBehaviour(SwitchAction.INVENTORY, SwitchAction.OFF, SINGLE_PRESS_REPEAT_DELAY_MS);
                        setDevicePowerPercentage(100);
                        updateTargetParameters();
                        break;
                    case ReadWrite:
                        setSwitchBehaviour(SwitchAction.INVENTORY, SwitchAction.BARCODE, SINGLE_PRESS_SINGLE_DELAY_MS);
                        setDevicePowerPercentage(10);
                        break;
                }
            });
        }
        catch (Exception e) {
            sendMessageNotification("Unable to perform action: " + e.getMessage());
        }
    }

    //change device switch action
    void setSwitchBehaviour(SwitchAction singlePress, SwitchAction doublePress, int singleDelay) {
        val switchActionCommand = SwitchActionCommand.synchronousCommand();
        switchActionCommand.setResetParameters(TriState.YES);
        switchActionCommand.setAsynchronousReportingEnabled(TriState.YES);
        switchActionCommand.setTakeNoAction(TriState.YES);
        switchActionCommand.setSinglePressAction(singlePress);
        switchActionCommand.setDoublePressAction(doublePress);
        switchActionCommand.setSinglePressRepeatDelay(singleDelay);
        getCommander().executeCommand(switchActionCommand);
    }

    //change power level
    int getDevicePowerPercentage() {
        //AntennaParameters.MaximumCarrierPower
        val minLevel = getCommander().getDeviceProperties().getMinimumCarrierPower();
        val maxLevel =  getCommander().getDeviceProperties().getMaximumCarrierPower();
        val powerCommand = InventoryCommand.synchronousCommand();
        getCommander().executeCommand(powerCommand);
        val powerLevel = powerCommand.getOutputPower();
        return Math.round((float)(powerLevel - minLevel) / (float)(maxLevel - minLevel) * 100F);
    }

    void setDevicePowerPercentage(int percentage) {
        val minLevel = getCommander().getDeviceProperties().getMinimumCarrierPower();
        val maxLevel =  getCommander().getDeviceProperties().getMaximumCarrierPower();
        val powerLevel = minLevel + percentage * ((float)(maxLevel - minLevel) / 100F);
        val powerCommand = InventoryCommand.synchronousCommand();
        powerCommand.setOutputPower((int)powerLevel);
        powerCommand.setTakeNoAction(TriState.YES);
        getCommander().executeCommand(powerCommand);
    }

    void setTargetTagEpc(String targetTagEpc) {
        if (targetTagEpc != null)
            this.targetTagEpc = targetTagEpc.toUpperCase();
    }

    int getBatteryLevel() {
        return batteryStatusResponder.getBatteryLevel();
    }

    // Reconfigure the Reader to target the current
    private void updateTargetParameters() {
        if (getCommander().isConnected()) {
            val invCmd = InventoryCommand.synchronousCommand();
            invCmd.setResetParameters(TriState.YES);
            invCmd.setTakeNoAction(TriState.YES);
            if(!StringHelper.isNullOrEmpty(targetTagEpc))
            {
                invCmd.setIncludeTransponderRssi(TriState.YES);
                invCmd.setQuerySession(QuerySession.SESSION_0);
                invCmd.setQueryTarget(QueryTarget.TARGET_B);
                invCmd.setInventoryOnly(TriState.NO);
                invCmd.setSelectData(targetTagEpc);
                invCmd.setSelectOffset(0x20);
                invCmd.setSelectLength(targetTagEpc.length() * 4);
                invCmd.setSelectAction(SelectAction.DEASSERT_SET_B_NOT_ASSERT_SET_A);
                invCmd.setSelectTarget(SelectTarget.SESSION_0);
                invCmd.setUseAlert(TriState.NO);
            }
            getCommander().executeCommand(invCmd);
            if (invCmd.isSuccessful())
                sendMessageNotification("updated\n");
            else
                sendMessageNotification("\n !!! update failed - ensure only hex characters used !!!\n");
        }
    }

    void scan() {
        testForAntenna();
        if (getCommander().isConnected()) {
            inventoryCommand.setTakeNoAction(TriState.NO);
            getCommander().executeCommand(inventoryCommand);
        }
    }

    void scanBarcode() {
        if (getCommander().isConnected()) {
            barcodeCommand.setTakeNoAction(TriState.NO);
            getCommander().executeCommand(barcodeCommand);
        }
    }

    // Set the parameters that are not user-specified
    private void setFixedWriteParameters() {
        writeCommand.setResetParameters(TriState.YES);
        writeCommand.setBank(Databank.ELECTRONIC_PRODUCT_CODE);

        // Set the data length
        if (writeCommand.getData() == null)
            writeCommand.setLength(0);
        else
            writeCommand.setLength(writeCommand.getData().length / 2);

        // Configure the select to match the given EPC
        // EPC is in hex and length is in bits
        val epcHex = writeCommand.getSelectData();
        if (epcHex != null) {
            // Only match the EPC value not the CRC or PC
            writeCommand.setSelectOffset(0x20);
            writeCommand.setSelectLength(epcHex.length() * 4);
        }

        writeCommand.setSelectAction(SelectAction.DEASSERT_SET_B_NOT_ASSERT_SET_A);
        writeCommand.setSelectTarget(SelectTarget.SESSION_2);

        writeCommand.setQuerySelect(QuerySelect.ALL);
        writeCommand.setQuerySession(QuerySession.SESSION_2);
        writeCommand.setQueryTarget(QueryTarget.TARGET_B);

        writeCommand.setTransponderReceivedDelegate((transponder, moreAvailable) -> {
            String eaMsg = transponder.getAccessErrorCode() == null ? "" : "\n" + transponder.getAccessErrorCode().getDescription() + " (EA)";
            String ebMsg = transponder.getBackscatterErrorCode() == null ? "" : "\n" + transponder.getBackscatterErrorCode().getDescription() + " (EB)";
            String errorMsg = eaMsg + ebMsg;
            if (errorMsg.length() > 0)
                errorMsg = "Error: " + errorMsg + "\n";

            sendMessageNotification(String.format(Locale.US,
                    "\nEPC: %s\nWords Written: %d of %d\n%s",
                    transponder.getEpc(),
                    transponder.getWordsWritten(),
                    writeCommand.getLength(),
                    errorMsg
            ));

            if (!moreAvailable)
                sendMessageNotification("\n");
        });
    }

    void write() {
        try {
            sendMessageNotification("\nWriting...\n");
            setFixedWriteParameters();
            performTask(() -> {
                getCommander().executeCommand(writeCommand);
                reportErrors(writeCommand);
                sendMessageNotification("Time taken: %.2fs" + getTaskExecutionDuration());
                //getCommander().addResponder(singleReadResponder);
            });
        } catch (Exception e) {
            sendMessageNotification("Unable to perform action: " + e.getMessage());
        }
    }

    //reset the reader configuration to default command values
    private void resetDevice() {
        val fdCommand = new FactoryDefaultsCommand();
        fdCommand.setResetParameters(TriState.YES);
        getCommander().executeCommand(fdCommand);
    }

    private void testForAntenna() {
        if (getCommander().isConnected()) {
            InventoryCommand testCommand = InventoryCommand.synchronousCommand();
            testCommand.setTakeNoAction(TriState.YES);
            getCommander().executeCommand(testCommand);
            if( !testCommand.isSuccessful())
                sendMessageNotification("ER:Error! Code: " + testCommand.getErrorCode() + " " + testCommand.getMessages().toString());
        }
    }

    private void reportErrors(AsciiSelfResponderCommandBase command) {
        if (command.isSuccessful())
            return;
        sendMessageNotification(String.format("%s failed!\nError code: %s\n", command.getClass().getSimpleName(), command.getErrorCode()));
        for (val message : command.getMessages())
            sendMessageNotification(message + "\n");
    }

    private int convertSignalToPercentage(int value) {
        int mRangeMaximum = -35;
        int mRangeMinimum = -70;

        if (value < mRangeMinimum) {
            mRangeMinimum = value;
        }
        if (value > mRangeMaximum) {
            mRangeMaximum = value;
        }

        return (100 * (value - mRangeMinimum)) / (mRangeMaximum - mRangeMinimum);
    }
}

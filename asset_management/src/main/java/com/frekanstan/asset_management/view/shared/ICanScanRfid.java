package com.frekanstan.asset_management.view.shared;

public interface ICanScanRfid {
    void onRfidScanned(String code);
    void onReaderStopped();
}
package com.frekanstan.asset_management.app.tracking;

import com.frekanstan.asset_management.view.MainActivityBase;

public interface IMountedBarcodeReaderManager {
    void scan(MainActivityBase context);

    void onResume();

    void onPause();
}

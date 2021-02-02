package com.frekanstan.asset_management.app.interfaces;

import android.view.View;

public interface IRecyclerClickListener {

    /**
     * Interface for Recycler View Click listener
     **/

    void onClick(View view, int position);

    void onLongClick(View view, int position);
}
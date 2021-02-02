package com.frekanstan.gateway_mobil.view.gateway;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.frekanstan.asset_management.app.sync.DateDeserializer;
import com.frekanstan.asset_management.app.webservice.AbpResult;
import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.app.connection.ServiceConnector;
import com.frekanstan.gateway_mobil.data.Receipt;
import com.frekanstan.gateway_mobil.databinding.ReceiptListFragmentBinding;
import com.frekanstan.gateway_mobil.view.MainActivity;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import lombok.val;

public class ReceiptListFragment extends Fragment {
    private MainActivity context;
    private ReceiptListFragmentBinding view;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            this.context = (MainActivity) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        val conn = ServiceConnector.getInstance(context);
        view = ReceiptListFragmentBinding.inflate(inflater, container, false);
        context.progDialog.setMessage("Fiş listesi yükleniyor");
        context.progDialog.show();
        if (requireArguments().getString("operation").equals("receive_assets"))
            conn.addToRequestQueue(conn.getAllOrderReceipts(this::onReceiptResponse));
        else if (requireArguments().getString("operation").equals("storage_transfer"))
            conn.addToRequestQueue(conn.getAllDemandReceipts(this::onReceiptResponse));
        return view.getRoot();
    }

    private void onReceiptResponse(AbpResult<ArrayList> response) {
        context.progDialog.hide();
        if (response.getResult().size() == 0)
            return;
        ArrayList<Receipt> receiptList = new ArrayList<>();
        val gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create();
        for (val obj : response.getResult())
            receiptList.add(gson.fromJson(gson.toJsonTree(obj).getAsJsonObject(), Receipt.class));
        Collections.sort(receiptList, Collections.reverseOrder());
        ReceiptListAdapter adapter = new ReceiptListAdapter(context, receiptList, requireArguments());
        view.receiptList.setLayoutManager(new LinearLayoutManager(context));
        view.receiptList.setAdapter(adapter);
        context.setFooterText(String.format(context.getLocale(), context.getString(R.string.number_receipt), receiptList.size()));
    }

    @Override
    public void onResume() {
        super.onResume();
        context.showHideFooter(true);
        context.actionButton.hide();
    }
}
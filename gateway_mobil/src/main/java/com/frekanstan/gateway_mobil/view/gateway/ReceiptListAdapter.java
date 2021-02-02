package com.frekanstan.gateway_mobil.view.gateway;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.frekanstan.gateway_mobil.R;
import com.frekanstan.gateway_mobil.data.Receipt;
import com.frekanstan.gateway_mobil.view.MainActivity;

import java.util.ArrayList;

import lombok.val;
import lombok.var;

import static com.frekanstan.asset_management.view.MainActivityBase.dateTimeFormat;

public class ReceiptListAdapter extends RecyclerView.Adapter<ReceiptListAdapter.ReceiptListViewHolder>
{
    private final MainActivity context;
    private final ArrayList<Receipt> receiptList;
    private final Bundle args;

    public ReceiptListAdapter(MainActivity context, ArrayList<Receipt> receiptList, Bundle args) {
        this.context = context;
        this.receiptList = receiptList;
        this.args = args;
    }

    @NonNull
    @Override
    public ReceiptListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.receipt_card, parent, false);
        var holder = new ReceiptListViewHolder(view);
        holder.setIsRecyclable(false);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ReceiptListAdapter.ReceiptListViewHolder holder, final int position) {
        final Receipt receipt = receiptList.get(position);
        if (receipt != null)
            holder.bindTo(receipt);
        else
            holder.clear();
    }

    class ReceiptListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView receiptNo, receiptDate, remoteId, currentAccountName;
        ImageView receiptIcon;

        ReceiptListViewHolder(View itemView) {
            super(itemView);
            receiptNo = itemView.findViewById(R.id.receipt_card_receipt_no);
            receiptDate = itemView.findViewById(R.id.receipt_card_receipt_date);
            remoteId = itemView.findViewById(R.id.receipt_card_remote_id);
            receiptIcon = itemView.findViewById(R.id.receipt_card_image);
            currentAccountName = itemView.findViewById(R.id.receipt_card_current_account_name);
            itemView.setOnClickListener(this);
        }

        void bindTo(Receipt receipt) {
            receiptIcon.setImageResource(R.drawable.document_gray_96px);
            receiptNo.setText(receipt.getReceiptNo());
            receiptDate.setText(dateTimeFormat.format(receipt.getReceiptDate()));
            remoteId.setText(String.valueOf(receipt.getRemoteId()));
            currentAccountName.setText(receipt.getCurrentAccountName());
        }

        void clear() {
            receiptNo.setText("");
            receiptDate.setText("");
            remoteId.setText("");
            currentAccountName.setText("");
        }

        @Override
        public void onClick(View v) {
            val lastClickedItem = receiptList.get(getAdapterPosition());
            val bundle = new Bundle();
            if (lastClickedItem != null) {
                bundle.putLong("receiptId", lastClickedItem.getRemoteId());
                bundle.putInt("warehouseId", lastClickedItem.getWarehouseId());
                bundle.putLong("currentAccountId", lastClickedItem.getCurrentAccountId());
                bundle.putString("operation", args.getString("operation"));
                context.nav.navigate(R.id.action_receiptListFragment_to_receiveAssetsFragment, bundle);
            }
        }
    }

    @Override
    public int getItemCount() {
        return receiptList.size();
    }
}

package com.floventy.blockcalls.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.floventy.blockcalls.R;
import com.floventy.blockcalls.data.BlockedCall;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying blocked call logs.
 */
public class BlockedCallAdapter extends ListAdapter<BlockedCall, BlockedCallAdapter.ViewHolder> {

    public BlockedCallAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<BlockedCall> DIFF_CALLBACK = new DiffUtil.ItemCallback<BlockedCall>() {
        @Override
        public boolean areItemsTheSame(@NonNull BlockedCall oldItem, @NonNull BlockedCall newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BlockedCall oldItem, @NonNull BlockedCall newItem) {
            return oldItem.getPhoneNumber().equals(newItem.getPhoneNumber()) &&
                    oldItem.getTimestamp() == newItem.getTimestamp() &&
                    oldItem.getMatchedPattern().equals(newItem.getMatchedPattern());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blocked_call, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedCall blockedCall = getItem(position);
        holder.bind(blockedCall);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textPhoneNumber;
        private final TextView textMatchedPattern;
        private final TextView textTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textPhoneNumber = itemView.findViewById(R.id.textPhoneNumber);
            textMatchedPattern = itemView.findViewById(R.id.textMatchedPattern);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
        }

        public void bind(BlockedCall blockedCall) {
            textPhoneNumber.setText(blockedCall.getPhoneNumber());
            textMatchedPattern.setText(
                    itemView.getContext().getString(R.string.matched_with, blockedCall.getMatchedPattern()));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            String dateStr = sdf.format(new Date(blockedCall.getTimestamp()));
            textTimestamp.setText(dateStr);
        }
    }
}

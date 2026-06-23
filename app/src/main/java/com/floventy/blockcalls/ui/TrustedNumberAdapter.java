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
import com.floventy.blockcalls.utils.TrustedNumbers;

/**
 * RecyclerView adapter for displaying trusted/safe numbers with their company names.
 */
public class TrustedNumberAdapter extends ListAdapter<TrustedNumbers.TrustedEntry, TrustedNumberAdapter.ViewHolder> {

    public TrustedNumberAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<TrustedNumbers.TrustedEntry> DIFF_CALLBACK = 
        new DiffUtil.ItemCallback<TrustedNumbers.TrustedEntry>() {
            @Override
            public boolean areItemsTheSame(@NonNull TrustedNumbers.TrustedEntry oldItem, @NonNull TrustedNumbers.TrustedEntry newItem) {
                return oldItem.numberOrPattern.equals(newItem.numberOrPattern);
            }

            @Override
            public boolean areContentsTheSame(@NonNull TrustedNumbers.TrustedEntry oldItem, @NonNull TrustedNumbers.TrustedEntry newItem) {
                return oldItem.numberOrPattern.equals(newItem.numberOrPattern)
                        && oldItem.name.equals(newItem.name);
            }
        };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trusted_number, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textNumber;
        private final TextView textName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textNumber = itemView.findViewById(R.id.textNumber);
            textName = itemView.findViewById(R.id.textName);
        }

        public void bind(TrustedNumbers.TrustedEntry entry) {
            textNumber.setText(entry.numberOrPattern);
            if (entry.name != null && !entry.name.isEmpty()) {
                textName.setText(entry.name);
                textName.setVisibility(View.VISIBLE);
            } else {
                textName.setVisibility(View.GONE);
            }
        }
    }
}

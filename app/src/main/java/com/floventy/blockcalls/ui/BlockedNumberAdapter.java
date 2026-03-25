package com.floventy.blockcalls.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.floventy.blockcalls.R;
import com.floventy.blockcalls.data.BlockedNumber;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying blocked number patterns / rules.
 * Each item shows the pattern, a subtitle with wildcard indicator and date,
 * and a toggle switch to enable / disable the rule.
 */
public class BlockedNumberAdapter extends ListAdapter<BlockedNumber, BlockedNumberAdapter.ViewHolder> {

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(BlockedNumber blockedNumber);

        void onItemLongClick(BlockedNumber blockedNumber, View anchor);

        void onToggleClick(BlockedNumber blockedNumber, boolean isEnabled);
    }

    public BlockedNumberAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<BlockedNumber> DIFF_CALLBACK = new DiffUtil.ItemCallback<BlockedNumber>() {
        @Override
        public boolean areItemsTheSame(@NonNull BlockedNumber oldItem, @NonNull BlockedNumber newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BlockedNumber oldItem, @NonNull BlockedNumber newItem) {
            return oldItem.getPattern().equals(newItem.getPattern())
                    && oldItem.isWildcard() == newItem.isWildcard()
                    && oldItem.getDateAdded() == newItem.getDateAdded()
                    && oldItem.isEnabled() == newItem.isEnabled();
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blocked_number, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textPattern;
        private final TextView textSubtitle;
        private final SwitchMaterial switchEnabled;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textPattern = itemView.findViewById(R.id.textPattern);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            switchEnabled = itemView.findViewById(R.id.switchEnabled);

            // Card click → open edit dialog
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });

            // Long click → show popup menu (edit / delete)
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemLongClick(getItem(position), v);
                }
                return true;
            });

            // Switch click → toggle rule enabled / disabled
            switchEnabled.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onToggleClick(getItem(position), switchEnabled.isChecked());
                }
            });
        }

        public void bind(BlockedNumber blockedNumber) {
            textPattern.setText(blockedNumber.getPattern());

            // Format date added
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(blockedNumber.getDateAdded()));

            // Subtitle: wildcard indicator + date
            if (blockedNumber.isWildcard()) {
                textSubtitle.setText("✱  " + dateStr);
                textSubtitle.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.accent));
            } else {
                textSubtitle.setText(dateStr);
                textSubtitle.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.text_hint));
            }

            // Set switch state without triggering listener
            switchEnabled.setChecked(blockedNumber.isEnabled());

            // Dim the entire row when the rule is disabled
            float alpha = blockedNumber.isEnabled() ? 1.0f : 0.45f;
            textPattern.setAlpha(alpha);
            textSubtitle.setAlpha(alpha);
        }
    }

    public BlockedNumber getItemAt(int position) {
        return getItem(position);
    }
}

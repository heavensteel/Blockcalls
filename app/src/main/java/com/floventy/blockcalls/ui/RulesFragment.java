package com.floventy.blockcalls.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.floventy.blockcalls.R;
import com.floventy.blockcalls.data.BlockedNumber;
import com.floventy.blockcalls.utils.CountryCodeHelper;
import com.floventy.blockcalls.utils.PatternMatcher;
import com.floventy.blockcalls.viewmodel.MainViewModel;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Fragment displaying the list of blocked number patterns (rules).
 */
public class RulesFragment extends Fragment {

    private MainViewModel viewModel;
    private BlockedNumberAdapter adapter;
    private View emptyState;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyState = view.findViewById(R.id.emptyState);
        recyclerView = view.findViewById(R.id.recyclerView);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);

        adapter = new BlockedNumberAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new BlockedNumberAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BlockedNumber blockedNumber) {
                showEditDialog(blockedNumber);
            }

            @Override
            public void onItemLongClick(BlockedNumber blockedNumber, View anchor) {
                showPopupMenu(blockedNumber, anchor);
            }

            @Override
            public void onToggleClick(BlockedNumber blockedNumber, boolean isEnabled) {
                viewModel.toggleBlockedNumber(blockedNumber, isEnabled);
            }
        });

        setupSwipeToDelete();

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getAllBlockedNumbers().observe(getViewLifecycleOwner(), blockedNumbers -> {
            adapter.submitList(blockedNumbers);
            if (blockedNumbers == null || blockedNumbers.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        fabAdd.setOnClickListener(v -> showAddPatternDialog());
    }

    private void showPopupMenu(BlockedNumber blockedNumber, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, R.string.edit);
        popup.getMenu().add(0, 2, 1, R.string.delete);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showEditDialog(blockedNumber);
                return true;
            } else if (item.getItemId() == 2) {
                showDeleteConfirmation(blockedNumber);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showAddPatternDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_pattern, null);
        TextInputEditText editCountryCode = dialogView.findViewById(R.id.editCountryCode);
        TextInputEditText editPattern = dialogView.findViewById(R.id.editPattern);

        // Auto-fill country code from SIM card
        String userCountry = CountryCodeHelper.getUserCountryCode(requireContext());
        CountryCodeHelper.CountryCode detectedCountry = CountryCodeHelper.findCountryByCode(userCountry);
        if (detectedCountry != null) {
            editCountryCode.setText("+" + detectedCountry.dialCode);
        } else {
            editCountryCode.setText("+90"); // Default to Turkey
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String countryCode = editCountryCode.getText() != null
                    ? editCountryCode.getText().toString().trim()
                    : "";
            String pattern = editPattern.getText() != null
                    ? editPattern.getText().toString().trim()
                    : "";

            if (pattern.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_empty_pattern, Toast.LENGTH_SHORT).show();
                return;
            }

            // When a country code is provided, strip the leading "0" from the local
            // pattern.
            // e.g. "+90" + "0850*" → "+90850*" (not "+900850*")
            // In Turkey (and most countries) the local leading 0 is dropped in
            // international format.
            String localPattern = (!countryCode.isEmpty() && pattern.startsWith("0"))
                    ? pattern.substring(1)
                    : pattern;
            String fullPattern = countryCode + localPattern;

            if (!PatternMatcher.isValidPattern(fullPattern)) {
                Toast.makeText(requireContext(), R.string.error_invalid_pattern, Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.addBlockedNumber(fullPattern);
            dialog.dismiss();
            Toast.makeText(requireContext(), R.string.pattern_added, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showEditDialog(BlockedNumber blockedNumber) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_pattern, null);
        TextInputEditText editCountryCode = dialogView.findViewById(R.id.editCountryCode);
        TextInputEditText editPattern = dialogView.findViewById(R.id.editPattern);

        // Split existing pattern into country code + rest
        String existingPattern = blockedNumber.getPattern();
        if (existingPattern.startsWith("+")) {
            // Try to extract dial code
            String dialCode = CountryCodeHelper.extractDialCodeFromPattern(existingPattern);
            if (dialCode != null) {
                editCountryCode.setText("+" + dialCode);
                editPattern.setText(existingPattern.substring(dialCode.length() + 1));
            } else {
                // Keep + sign in country code field, rest in pattern
                editCountryCode.setText("+");
                editPattern.setText(existingPattern.substring(1));
            }
        } else {
            // No country code - clear the field
            editCountryCode.setText("");
            editPattern.setText(existingPattern);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        if (tvTitle != null) {
            tvTitle.setText(R.string.dialog_edit_pattern_title);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String countryCode = editCountryCode.getText() != null
                    ? editCountryCode.getText().toString().trim()
                    : "";
            String pattern = editPattern.getText() != null
                    ? editPattern.getText().toString().trim()
                    : "";

            if (pattern.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_empty_pattern, Toast.LENGTH_SHORT).show();
                return;
            }

            // When a country code is provided, strip the leading "0" from the local
            // pattern.
            // e.g. "+90" + "0850*" → "+90850*" (not "+900850*")
            String localPattern = (!countryCode.isEmpty() && pattern.startsWith("0"))
                    ? pattern.substring(1)
                    : pattern;
            String fullPattern = countryCode + localPattern;

            if (!PatternMatcher.isValidPattern(fullPattern)) {
                Toast.makeText(requireContext(), R.string.error_invalid_pattern, Toast.LENGTH_SHORT).show();
                return;
            }

            blockedNumber.setPattern(fullPattern);
            viewModel.updateBlockedNumber(blockedNumber);
            dialog.dismiss();
            Toast.makeText(requireContext(), R.string.pattern_updated, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showDeleteConfirmation(BlockedNumber blockedNumber) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_pattern_title)
                .setMessage(R.string.delete_pattern_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteBlockedNumber(blockedNumber);
                    Toast.makeText(requireContext(), R.string.pattern_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                BlockedNumber blockedNumber = adapter.getItemAt(position);

                viewModel.deleteBlockedNumber(blockedNumber);

                Snackbar.make(recyclerView, R.string.pattern_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, v -> viewModel.addBlockedNumber(blockedNumber.getPattern()))
                        .show();
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }
}

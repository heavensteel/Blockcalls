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
    private TrustedNumberAdapter trustedAdapter;
    private View emptyState;
    private RecyclerView recyclerView;
    private com.google.android.material.tabs.TabLayout tabLayout;
    private ItemTouchHelper itemTouchHelper;

    private android.widget.ImageView emptyStateIcon;
    private TextView emptyStateTitle;
    private TextView emptyStateDesc;

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
        emptyStateIcon = view.findViewById(R.id.emptyStateIcon);
        emptyStateTitle = view.findViewById(R.id.emptyStateTitle);
        emptyStateDesc = view.findViewById(R.id.emptyStateDesc);
        recyclerView = view.findViewById(R.id.recyclerView);
        tabLayout = view.findViewById(R.id.tabLayout);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        FloatingActionButton fabBlockLists = view.findViewById(R.id.fabBlockLists);

        adapter = new BlockedNumberAdapter();
        trustedAdapter = new TrustedNumberAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup swipe-to-delete helper (doesn't attach yet)
        setupSwipeToDelete();

        // Setup TabLayout
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_blocked_rules));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_safe_numbers));

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                switchTab(tab.getPosition(), fabAdd, fabBlockLists);
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

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

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getAllBlockedNumbers().observe(getViewLifecycleOwner(), blockedNumbers -> {
            if (tabLayout.getSelectedTabPosition() == 0) {
                adapter.submitList(blockedNumbers);
                updateBlockedEmptyState(blockedNumbers == null || blockedNumbers.isEmpty());
            }
        });

        // Initialize with default Tab 0
        switchTab(0, fabAdd, fabBlockLists);

        fabAdd.setOnClickListener(v -> showAddPatternDialog());
        fabBlockLists.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    requireContext(), BlockListsActivity.class);
            startActivity(intent);
        });
    }

    private void switchTab(int position, FloatingActionButton fabAdd, FloatingActionButton fabBlockLists) {
        if (position == 0) {
            // Blocked Rules
            recyclerView.setAdapter(adapter);
            if (itemTouchHelper != null) {
                itemTouchHelper.attachToRecyclerView(recyclerView);
            }
            fabAdd.show();
            fabBlockLists.show();
            
            java.util.List<BlockedNumber> current = viewModel.getAllBlockedNumbers().getValue();
            adapter.submitList(current);
            updateBlockedEmptyState(current == null || current.isEmpty());
        } else {
            // Safe Numbers
            recyclerView.setAdapter(trustedAdapter);
            if (itemTouchHelper != null) {
                itemTouchHelper.attachToRecyclerView(null);
            }
            fabAdd.hide();
            fabBlockLists.hide();

            java.util.List<com.floventy.blockcalls.utils.TrustedNumbers.TrustedEntry> safeList = 
                    com.floventy.blockcalls.utils.TrustedNumbers.getAllTrustedEntries();
            trustedAdapter.submitList(safeList);
            updateSafeEmptyState(safeList == null || safeList.isEmpty());
        }
    }

    private void updateBlockedEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateIcon.setImageResource(android.R.drawable.ic_menu_call);
            emptyStateTitle.setText(R.string.no_blocked_numbers);
            emptyStateDesc.setText(R.string.add_pattern_hint);
            emptyStateDesc.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateSafeEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateIcon.setImageResource(android.R.drawable.ic_lock_power_off);
            emptyStateTitle.setText(R.string.no_safe_numbers);
            emptyStateDesc.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
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
        TextInputEditText editNote = dialogView.findViewById(R.id.editNote);

        String userCountry = CountryCodeHelper.getUserCountryCode(requireContext());
        CountryCodeHelper.CountryCode detectedCountry = CountryCodeHelper.findCountryByCode(userCountry);
        if (detectedCountry != null) {
            editCountryCode.setText("+" + detectedCountry.dialCode);
        } else {
            editCountryCode.setText("+90");
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
            String note = editNote.getText() != null
                    ? editNote.getText().toString().trim()
                    : "";

            if (pattern.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_empty_pattern, Toast.LENGTH_SHORT).show();
                return;
            }

            String localPattern = (!countryCode.isEmpty() && pattern.startsWith("0"))
                    ? pattern.substring(1)
                    : pattern;
            String fullPattern = countryCode + localPattern;

            if (!PatternMatcher.isValidPattern(fullPattern)) {
                Toast.makeText(requireContext(), R.string.error_invalid_pattern, Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.addBlockedNumber(fullPattern, note);
            dialog.dismiss();
            Toast.makeText(requireContext(), R.string.pattern_added, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showEditDialog(BlockedNumber blockedNumber) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_pattern, null);
        TextInputEditText editCountryCode = dialogView.findViewById(R.id.editCountryCode);
        TextInputEditText editPattern = dialogView.findViewById(R.id.editPattern);
        TextInputEditText editNote = dialogView.findViewById(R.id.editNote);

        String existingPattern = blockedNumber.getPattern();
        if (existingPattern.startsWith("+")) {
            String dialCode = CountryCodeHelper.extractDialCodeFromPattern(existingPattern);
            if (dialCode != null) {
                editCountryCode.setText("+" + dialCode);
                editPattern.setText(existingPattern.substring(dialCode.length() + 1));
            } else {
                editCountryCode.setText("+");
                editPattern.setText(existingPattern.substring(1));
            }
        } else {
            editCountryCode.setText("");
            editPattern.setText(existingPattern);
        }

        if (blockedNumber.getNote() != null) {
            editNote.setText(blockedNumber.getNote());
        } else {
            editNote.setText("");
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
            String note = editNote.getText() != null
                    ? editNote.getText().toString().trim()
                    : "";

            if (pattern.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_empty_pattern, Toast.LENGTH_SHORT).show();
                return;
            }

            String localPattern = (!countryCode.isEmpty() && pattern.startsWith("0"))
                    ? pattern.substring(1)
                    : pattern;
            String fullPattern = countryCode + localPattern;

            if (!PatternMatcher.isValidPattern(fullPattern)) {
                Toast.makeText(requireContext(), R.string.error_invalid_pattern, Toast.LENGTH_SHORT).show();
                return;
            }

            blockedNumber.setPattern(fullPattern);
            blockedNumber.setNote(note);
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
                        .setAction(R.string.undo, v -> viewModel.addBlockedNumber(blockedNumber))
                        .show();
            }
        };

        itemTouchHelper = new ItemTouchHelper(simpleCallback);
    }
}

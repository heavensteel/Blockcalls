package com.floventy.blockcalls.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.floventy.blockcalls.R;
import com.floventy.blockcalls.viewmodel.BlockedCallsViewModel;

/**
 * Fragment displaying the log of blocked calls.
 */
public class BlockedCallsFragment extends Fragment {

    private BlockedCallsViewModel viewModel;
    private BlockedCallAdapter adapter;
    private View emptyState;
    private RecyclerView recyclerView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blocked_calls, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        emptyState = view.findViewById(R.id.emptyState);
        recyclerView = view.findViewById(R.id.recyclerView);

        // Setup RecyclerView
        adapter = new BlockedCallAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(BlockedCallsViewModel.class);
        viewModel.getAllBlockedCalls().observe(getViewLifecycleOwner(), blockedCalls -> {
            adapter.submitList(blockedCalls);

            // Show/hide empty state
            if (blockedCalls == null || blockedCalls.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_blocked_calls, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_log) {
            showClearLogConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearLogConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_log)
                .setMessage(R.string.confirm_clear_log)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    viewModel.clearAllBlockedCalls();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}

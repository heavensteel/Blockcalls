package com.floventy.blockcalls.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.floventy.blockcalls.R;
import com.floventy.blockcalls.viewmodel.BlockedCallsViewModel;

/**
 * Activity displaying the log of blocked calls.
 */
public class BlockedCallsActivity extends AppCompatActivity {
    
    private BlockedCallsViewModel viewModel;
    private BlockedCallAdapter adapter;
    private View emptyState;
    private RecyclerView recyclerView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_calls);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize views
        emptyState = findViewById(R.id.emptyState);
        recyclerView = findViewById(R.id.recyclerView);
        
        // Setup RecyclerView
        adapter = new BlockedCallAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(BlockedCallsViewModel.class);
        viewModel.getAllBlockedCalls().observe(this, blockedCalls -> {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_blocked_calls, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_clear_log) {
            showClearLogConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showClearLogConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.clear_log)
            .setMessage(R.string.confirm_clear_log)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                viewModel.clearAllBlockedCalls();
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }
}

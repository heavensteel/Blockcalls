package com.floventy.blockcalls.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.floventy.blockcalls.R;
import com.floventy.blockcalls.data.PreMadeBlockLists;
import com.floventy.blockcalls.utils.BlockListsFetcher;
import com.floventy.blockcalls.viewmodel.MainViewModel;

import java.util.List;

/**
 * Shows a list of countries. Tap a country → pick categories to bulk-add rules.
 */
public class BlockListsActivity extends AppCompatActivity {

    private View layoutLoading, layoutError, recyclerCountries;
    private TextView tvError;
    private MainViewModel viewModel;
    private final BlockListsFetcher fetcher = new BlockListsFetcher();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_lists);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutLoading = findViewById(R.id.layoutLoading);
        layoutError = findViewById(R.id.layoutError);
        recyclerCountries = findViewById(R.id.recyclerCountries);
        tvError = findViewById(R.id.tvError);

        findViewById(R.id.btnRetry).setOnClickListener(v -> loadLists());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        loadLists();
    }

    private void loadLists() {
        showState(State.LOADING);

        fetcher.fetch(this, new BlockListsFetcher.Callback() {
            @Override
            public void onSuccess(List<PreMadeBlockLists.Country> countries) {
                runOnUiThread(() -> showCountries(countries));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    tvError.setText(message);
                    showState(State.ERROR);
                });
            }
        });
    }

    private void showCountries(List<PreMadeBlockLists.Country> countries) {
        RecyclerView rv = (RecyclerView) recyclerCountries;
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new CountryAdapter(countries, country -> showCategoryDialog(country)));
        showState(State.LIST);
    }

    private void showCategoryDialog(PreMadeBlockLists.Country country) {
        List<PreMadeBlockLists.Category> cats = country.categories;
        String[] names = new String[cats.size()];
        boolean[] checked = new boolean[cats.size()];
        int[] totalPatterns = new int[cats.size()];

        for (int i = 0; i < cats.size(); i++) {
            PreMadeBlockLists.Category c = cats.get(i);
            totalPatterns[i] = c.patterns.size();
            names[i] = c.name + "  (" + totalPatterns[i] + " kural)";
        }

        new AlertDialog.Builder(this)
                .setTitle(country.getDisplayName())
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("Seçilenleri Ekle", (dialog, which) -> {
                    int totalAdded = 0;
                    for (int i = 0; i < cats.size(); i++) {
                        if (checked[i]) {
                            for (String pattern : cats.get(i).patterns) {
                                viewModel.addBlockedNumber(pattern);
                                totalAdded++;
                            }
                        }
                    }
                    if (totalAdded > 0) {
                        Toast.makeText(this, "✅ " + totalAdded + " kural eklendi", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Hiçbir kategori seçilmedi", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .setNeutralButton("Tümünü Seç", null) // handled below
                .show();

        // "Tümünü Seç" resets and selects all, then shows dialog again
        // Handled via a simpler approach: just show as is
    }

    // ─── State machine ───────────────────────────────────────────────────────

    private enum State { LOADING, ERROR, LIST }

    private void showState(State s) {
        layoutLoading.setVisibility(s == State.LOADING ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(s == State.ERROR ? View.VISIBLE : View.GONE);
        recyclerCountries.setVisibility(s == State.LIST ? View.VISIBLE : View.GONE);
    }

    // ─── Inner adapter ───────────────────────────────────────────────────────

    interface OnCountryClick { void onClick(PreMadeBlockLists.Country country); }

    static class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.VH> {
        private final List<PreMadeBlockLists.Country> items;
        private final OnCountryClick listener;

        CountryAdapter(List<PreMadeBlockLists.Country> items, OnCountryClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_country, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            PreMadeBlockLists.Country c = items.get(pos);
            h.flag.setText(c.flag);
            h.name.setText(c.name);
            int total = 0;
            for (PreMadeBlockLists.Category cat : c.categories) total += cat.patterns.size();
            h.count.setText(c.categories.size() + " kategori • " + total + " kural");
            h.itemView.setOnClickListener(v -> listener.onClick(c));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView flag, name, count;
            VH(View v) {
                super(v);
                flag = v.findViewById(R.id.tvFlag);
                name = v.findViewById(R.id.tvCountryName);
                count = v.findViewById(R.id.tvCategoryCount);
            }
        }
    }
}

package com.floventy.blockcalls.utils;

import android.content.Context;
import android.util.Log;

import com.floventy.blockcalls.data.PreMadeBlockLists;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches block lists JSON from GitHub and caches locally.
 * URL: https://raw.githubusercontent.com/heavensteel/Blockcalls/main/block_lists/block_lists.json
 */
public class BlockListsFetcher {

    private static final String TAG = "BlockListsFetcher";
    private static final String JSON_URL =
        "https://raw.githubusercontent.com/heavensteel/Blockcalls/main/block_lists/block_lists.json";
    private static final String CACHE_FILE = "block_lists_cache.json";
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours

    public interface Callback {
        void onSuccess(List<PreMadeBlockLists.Country> countries);
        void onError(String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void fetch(Context context, Callback callback) {
        executor.execute(() -> {
            // Try cache first if fresh
            String cached = readCache(context);
            if (cached != null) {
                List<PreMadeBlockLists.Country> countries = parse(cached);
                if (countries != null && !countries.isEmpty()) {
                    callback.onSuccess(countries);
                    return;
                }
            }

            // Fetch from network
            try {
                String json = download(JSON_URL);
                if (json != null) {
                    writeCache(context, json);
                    List<PreMadeBlockLists.Country> countries = parse(json);
                    if (countries != null) {
                        callback.onSuccess(countries);
                    } else {
                        callback.onError("Veri ayrıştırılamadı.");
                    }
                } else {
                    callback.onError("İndirme başarısız.");
                }
            } catch (Exception e) {
                Log.w(TAG, "Network fetch failed, trying stale cache", e);
                // Fall back to stale cache
                String stale = readCacheIgnoreAge(context);
                if (stale != null) {
                    List<PreMadeBlockLists.Country> countries = parse(stale);
                    if (countries != null && !countries.isEmpty()) {
                        callback.onSuccess(countries);
                        return;
                    }
                }
                callback.onError("İnternet bağlantısı yok ve önbellekte veri bulunamadı.");
            }
        });
    }

    // ─── Network ────────────────────────────────────────────────────────────

    private String download(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "HTTP " + code + " for " + urlStr);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            reader.close();
            return sb.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ─── Cache ──────────────────────────────────────────────────────────────

    private String readCache(Context ctx) {
        File f = new File(ctx.getFilesDir(), CACHE_FILE);
        if (!f.exists()) return null;
        long age = System.currentTimeMillis() - f.lastModified();
        if (age > CACHE_TTL_MS) return null; // stale
        return readFile(f);
    }

    private String readCacheIgnoreAge(Context ctx) {
        File f = new File(ctx.getFilesDir(), CACHE_FILE);
        if (!f.exists()) return null;
        return readFile(f);
    }

    private void writeCache(Context ctx, String json) {
        File f = new File(ctx.getFilesDir(), CACHE_FILE);
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(json);
        } catch (Exception e) {
            Log.w(TAG, "Cache write failed", e);
        }
    }

    private String readFile(File f) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Parsing ─────────────────────────────────────────────────────────────

    private List<PreMadeBlockLists.Country> parse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray countriesJson = root.getJSONArray("countries");
            List<PreMadeBlockLists.Country> countries = new ArrayList<>();

            for (int i = 0; i < countriesJson.length(); i++) {
                JSONObject cj = countriesJson.getJSONObject(i);
                String code = cj.getString("code");
                String name = cj.getString("name");
                String flag = cj.getString("flag");
                JSONArray catsJson = cj.getJSONArray("categories");

                List<PreMadeBlockLists.Category> categories = new ArrayList<>();
                for (int j = 0; j < catsJson.length(); j++) {
                    JSONObject cat = catsJson.getJSONObject(j);
                    String id = cat.getString("id");
                    String catName = cat.getString("name");
                    String desc = cat.optString("description", "");
                    JSONArray patsJson = cat.getJSONArray("patterns");

                    List<String> patterns = new ArrayList<>();
                    for (int k = 0; k < patsJson.length(); k++) {
                        patterns.add(patsJson.getString(k));
                    }
                    categories.add(new PreMadeBlockLists.Category(id, catName, desc, patterns));
                }
                countries.add(new PreMadeBlockLists.Country(code, name, flag, categories));
            }
            return countries;
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error", e);
            return null;
        }
    }
}

package com.floventy.blockcalls.utils;

import android.content.Context;
import android.util.Log;

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
 * Fetches the trusted/safe numbers whitelist JSON from GitHub and caches it locally.
 * URL: https://raw.githubusercontent.com/heavensteel/Blockcalls/main/block_lists/safe_lists.json
 */
public class TrustedListsFetcher {

    private static final String TAG = "TrustedListsFetcher";
    private static final String JSON_URL =
            "https://raw.githubusercontent.com/heavensteel/Blockcalls/main/block_lists/safe_lists.json";
    private static final String CACHE_FILE = "safe_lists_cache.json";
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours

    public interface Callback {
        void onSuccess(List<String> prefixes, List<String> exact);
        void onError(String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void fetch(Context context, Callback callback) {
        executor.execute(() -> {
            // Try cache first if fresh
            String cached = readCache(context);
            if (cached != null) {
                ParsedLists lists = parse(cached);
                if (lists != null) {
                    callback.onSuccess(lists.prefixes, lists.exact);
                    return;
                }
            }

            // Fetch from network
            try {
                String json = download(JSON_URL);
                if (json != null) {
                    writeCache(context, json);
                    ParsedLists lists = parse(json);
                    if (lists != null) {
                        callback.onSuccess(lists.prefixes, lists.exact);
                    } else {
                        callback.onError("Safe list veri ayrıştırılamadı.");
                    }
                } else {
                    callback.onError("Safe list indirme başarısız.");
                }
            } catch (Exception e) {
                Log.w(TAG, "Network fetch failed for safe list, trying stale cache", e);
                // Fall back to stale cache
                String stale = readCacheIgnoreAge(context);
                if (stale != null) {
                    ParsedLists lists = parse(stale);
                    if (lists != null) {
                        callback.onSuccess(lists.prefixes, lists.exact);
                        return;
                    }
                }
                callback.onError("İnternet bağlantısı yok ve önbellekte safe list bulunamadı.");
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
                Log.w(TAG, "HTTP " + code + " for safe list " + urlStr);
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
            Log.w(TAG, "Cache write failed for safe list", e);
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

    private ParsedLists parse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            
            JSONArray prefixesJson = root.optJSONArray("prefixes");
            List<String> prefixes = new ArrayList<>();
            if (prefixesJson != null) {
                for (int i = 0; i < prefixesJson.length(); i++) {
                    prefixes.add(prefixesJson.getString(i));
                }
            }

            JSONArray exactJson = root.optJSONArray("exact");
            List<String> exact = new ArrayList<>();
            if (exactJson != null) {
                for (int i = 0; i < exactJson.length(); i++) {
                    exact.add(exactJson.getString(i));
                }
            }

            return new ParsedLists(prefixes, exact);
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error for safe list", e);
            return null;
        }
    }

    public static class ParsedLists {
        public final List<String> prefixes;
        public final List<String> exact;

        public ParsedLists(List<String> prefixes, List<String> exact) {
            this.prefixes = prefixes;
            this.exact = exact;
        }
    }
}

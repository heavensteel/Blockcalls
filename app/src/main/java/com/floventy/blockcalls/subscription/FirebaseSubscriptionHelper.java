package com.floventy.blockcalls.subscription;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks subscription status from Firestore.
 *
 * Firestore document: /subscribers/{email}
 *   active:         boolean  — true = can use app
 *   trial:          boolean  — true = on free trial
 *   trialEndsAt:    Timestamp — when trial expires (set on first login)
 *   plan:           string   — "monthly" | "sixmonth" | "yearly" (set after purchase)
 *
 * Logic:
 *   1. Document doesn't exist → create trial (7 days), active = true
 *   2. Document exists, active = true, trial = false → paid subscriber
 *   3. Document exists, trial = true → check trialEndsAt; if past → active = false
 *   4. Document exists, active = false → blocked
 */
public class FirebaseSubscriptionHelper {

    private static final String TAG = "FirebaseSubHelper";
    private static final String AUTH_PREFS = "auth_prefs";
    public static final String KEY_USER_EMAIL = "user_email";

    private static final int TRIAL_DAYS = 7;

    public interface SubscriptionCallback {
        /** isActive=true → let user in; trialDaysLeft≥0 when on trial, -1 when paid */
        void onResult(boolean isActive, int trialDaysLeft);
        void onError(Exception e);
    }

    public static void checkSubscription(String email, SubscriptionCallback callback) {
        if (email == null || email.isEmpty()) {
            callback.onResult(false, -1);
            return;
        }

        String docId = email.toLowerCase().trim();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("subscribers").document(docId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        // New user — start 7-day trial
                        startTrial(docId, callback);
                        return;
                    }

                    boolean active = Boolean.TRUE.equals(document.getBoolean("active"));
                    boolean isTrial = Boolean.TRUE.equals(document.getBoolean("trial"));

                    if (!active) {
                        // Manually blocked or subscription cancelled
                        callback.onResult(false, -1);
                        return;
                    }

                    if (!isTrial) {
                        // Paid subscriber
                        callback.onResult(true, -1);
                        return;
                    }

                    // Trial user — check expiry
                    Timestamp trialEndsAt = document.getTimestamp("trialEndsAt");
                    if (trialEndsAt == null) {
                        callback.onResult(true, TRIAL_DAYS);
                        return;
                    }

                    long now = System.currentTimeMillis();
                    long endsMs = trialEndsAt.toDate().getTime();
                    long daysLeft = (endsMs - now) / (1000L * 60 * 60 * 24);

                    if (daysLeft < 0) {
                        // Trial expired — update Firestore and block
                        db.collection("subscribers").document(docId)
                                .update("active", false)
                                .addOnSuccessListener(v ->
                                        Log.d(TAG, "Trial expired for " + docId));
                        callback.onResult(false, 0);
                    } else {
                        callback.onResult(true, (int) daysLeft);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Firestore check failed for " + docId, e);
                    callback.onError(e);
                });
    }

    private static void startTrial(String docId, SubscriptionCallback callback) {
        long trialEndMs = System.currentTimeMillis() + (long) TRIAL_DAYS * 24 * 60 * 60 * 1000;
        Timestamp trialEndsAt = new Timestamp(new Date(trialEndMs));

        Map<String, Object> data = new HashMap<>();
        data.put("active", true);
        data.put("trial", true);
        data.put("trialEndsAt", trialEndsAt);
        data.put("trialStartedAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("subscribers")
                .document(docId)
                .set(data)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Trial started for " + docId + ", ends: " + trialEndsAt.toDate());
                    callback.onResult(true, TRIAL_DAYS);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to create trial for " + docId, e);
                    callback.onError(e);
                });
    }

    public static void saveEmail(Context context, String email) {
        context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_EMAIL, email.toLowerCase().trim())
                .apply();
    }

    public static String getSavedEmail(Context context) {
        return context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_USER_EMAIL, null);
    }

    public static void logout(Context context) {
        context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_USER_EMAIL)
                .apply();
        context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_premium", false)
                .apply();
    }

    public static void savePremiumStatus(Context context, boolean isPremium) {
        context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_premium", isPremium)
                .apply();
    }
}

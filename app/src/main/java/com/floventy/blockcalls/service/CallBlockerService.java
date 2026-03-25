package com.floventy.blockcalls.service;

import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.floventy.blockcalls.data.AppDatabase;
import com.floventy.blockcalls.data.BlockedCall;
import com.floventy.blockcalls.data.BlockedNumber;
import com.floventy.blockcalls.subscription.SubscriptionManager;
import com.floventy.blockcalls.utils.ContactChecker;
import com.floventy.blockcalls.utils.PatternMatcher;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service that screens incoming calls and blocks numbers matching patterns.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class CallBlockerService extends CallScreeningService {

    private static final String TAG = "CallBlockerService";
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {
        String incomingNumber = getPhoneNumber(callDetails);

        Log.d(TAG, "Screening call from: " + incomingNumber);

        if (incomingNumber == null || incomingNumber.isEmpty()) {
            // Allow calls with no caller ID
            respondToCall(callDetails, new CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .build());
            return;
        }

        // Check against block list in background thread
        executorService.execute(() -> {
            boolean shouldBlock = checkBlockList(incomingNumber);

            if (shouldBlock) {
                Log.d(TAG, "Blocking call from: " + incomingNumber);

                // Block and reject the call
                CallResponse response = new CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build();

                respondToCall(callDetails, response);
            } else {
                // Allow the call
                respondToCall(callDetails, new CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .build());
            }
        });
    }

    /**
     * Extract phone number from call details.
     * Handles URL-encoded characters (e.g., %2B for +) in the tel: URI.
     */
    private String getPhoneNumber(Call.Details callDetails) {
        if (callDetails.getHandle() != null) {
            String uri = callDetails.getHandle().toString();
            // URL decode to handle %2B -> + etc.
            try {
                uri = java.net.URLDecoder.decode(uri, "UTF-8");
            } catch (Exception e) {
                Log.w(TAG, "Failed to URL decode: " + uri);
            }
            // Remove "tel:" prefix if present
            if (uri.startsWith("tel:")) {
                uri = uri.substring(4);
            }
            return uri.trim();
        }
        return null;
    }

    /**
     * Check if the incoming number matches any pattern in the block list.
     * This runs on a background thread.
     */
    private boolean checkBlockList(String phoneNumber) {
        try {
            // Lightweight cached subscription check - avoids creating BillingClient per
            // call
            if (!SubscriptionManager.isAppPremium(getApplicationContext())) {
                Log.d(TAG, "No active subscription, not blocking calls");
                return false;
            }

            // Skip blocking if the number is saved in contacts
            if (ContactChecker.isContactSaved(getApplicationContext(), phoneNumber)) {
                Log.d(TAG, "Number is a saved contact, allowing call: " + phoneNumber);
                return false;
            }

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<BlockedNumber> blockedNumbers = db.blockedNumberDao().getAllEnabledBlockedNumbersSync();

            for (BlockedNumber blockedNumber : blockedNumbers) {
                if (PatternMatcher.matches(phoneNumber, blockedNumber.getPattern(), blockedNumber.isWildcard())) {
                    // Log the blocked call
                    logBlockedCall(phoneNumber, blockedNumber.getPattern());
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking block list", e);
            return false;
        }
    }

    /**
     * Log a blocked call to the database.
     */
    private void logBlockedCall(String phoneNumber, String matchedPattern) {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            BlockedCall blockedCall = new BlockedCall(
                    phoneNumber,
                    System.currentTimeMillis(),
                    matchedPattern);
            db.blockedCallDao().insert(blockedCall);
            Log.d(TAG, "Logged blocked call: " + phoneNumber + " (matched: " + matchedPattern + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error logging blocked call", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

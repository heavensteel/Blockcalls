package com.floventy.blockcalls.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.floventy.blockcalls.data.AppDatabase;
import com.floventy.blockcalls.data.BlockedCall;
import com.floventy.blockcalls.data.BlockedNumber;
import com.floventy.blockcalls.subscription.SubscriptionManager;
import com.floventy.blockcalls.utils.ContactChecker;
import com.floventy.blockcalls.utils.PatternMatcher;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fallback BroadcastReceiver for blocking calls on devices that don't support
 * CallScreeningService / ROLE_CALL_SCREENING (e.g., rooted phones, custom ROMs,
 * some international OEM devices).
 * 
 * Uses the legacy PHONE_STATE broadcast to detect incoming calls and
 * ITelephony.endCall() via reflection to terminate blocked calls.
 */
public class CallBlockerReceiver extends BroadcastReceiver {

    private static final String TAG = "CallBlockerReceiver";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (!TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            return;
        }

        // Get the incoming phone number
        String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (incomingNumber == null || incomingNumber.isEmpty()) {
            Log.d(TAG, "No incoming number available");
            return;
        }

        Log.d(TAG, "Incoming call detected from: " + incomingNumber);

        // Check block list in background
        executorService.execute(() -> {
            try {
                // Lightweight cached subscription check - avoids creating BillingClient per
                // call
                if (!SubscriptionManager.isAppPremium(context.getApplicationContext())) {
                    Log.d(TAG, "No active subscription, not blocking calls");
                    return;
                }

                // Skip blocking if the number is saved in contacts
                if (ContactChecker.isContactSaved(context.getApplicationContext(), incomingNumber)) {
                    Log.d(TAG, "Number is a saved contact, allowing call: " + incomingNumber);
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
                List<BlockedNumber> blockedNumbers = db.blockedNumberDao().getAllEnabledBlockedNumbersSync();

                for (BlockedNumber blockedNumber : blockedNumbers) {
                    if (PatternMatcher.matches(incomingNumber, blockedNumber.getPattern(),
                            blockedNumber.isWildcard())) {
                        Log.d(TAG, "Blocking call from: " + incomingNumber + " (matched: " + blockedNumber.getPattern()
                                + ")");

                        // End the call
                        endCall(context);

                        // Log the blocked call
                        logBlockedCall(context, incomingNumber, blockedNumber.getPattern());
                        return;
                    }
                }

                Log.d(TAG, "Call allowed from: " + incomingNumber);
            } catch (Exception e) {
                Log.e(TAG, "Error checking block list", e);
            }
        });
    }

    /**
     * End the current incoming call using TelephonyManager reflection.
     * This works on rooted devices and some non-rooted devices depending on OEM.
     */
    private void endCall(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Try using ITelephony via reflection
            Method getITelephony = telephonyManager.getClass().getDeclaredMethod("getITelephony");
            getITelephony.setAccessible(true);
            Object iTelephony = getITelephony.invoke(telephonyManager);

            if (iTelephony != null) {
                Method endCallMethod = iTelephony.getClass().getDeclaredMethod("endCall");
                endCallMethod.invoke(iTelephony);
                Log.d(TAG, "Call ended successfully via ITelephony");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to end call via ITelephony reflection: " + e.getMessage());

            // Alternative: Try using telecom service on Android 9+
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.telecom.TelecomManager telecomManager = (android.telecom.TelecomManager) context
                            .getSystemService(Context.TELECOM_SERVICE);
                    if (telecomManager != null) {
                        Method endCallMethod = telecomManager.getClass().getDeclaredMethod("endCall");
                        endCallMethod.invoke(telecomManager);
                        Log.d(TAG, "Call ended successfully via TelecomManager");
                    }
                }
            } catch (Exception e2) {
                Log.e(TAG, "Failed to end call via TelecomManager: " + e2.getMessage());
            }
        }
    }

    /**
     * Log a blocked call to the database.
     */
    private void logBlockedCall(Context context, String phoneNumber, String matchedPattern) {
        try {
            AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
            BlockedCall blockedCall = new BlockedCall(
                    phoneNumber,
                    System.currentTimeMillis(),
                    matchedPattern);
            db.blockedCallDao().insert(blockedCall);
            Log.d(TAG, "Logged blocked call: " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error logging blocked call", e);
        }
    }
}

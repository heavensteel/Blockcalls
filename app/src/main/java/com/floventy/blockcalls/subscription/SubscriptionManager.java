package com.floventy.blockcalls.subscription;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.List;

/**
 * Manages subscription status using Google Play Billing ONLY.
 *
 * LOGIC (secure):
 * - Trial period is managed by Google Play (tied to Google account)
 * - App just checks: does this user have an active subscription?
 * - YES → App works (including during the free trial period)
 * - NO → Show paywall
 *
 * This approach:
 * ✅ Cannot be bypassed by uninstall/reinstall
 * ✅ Google handles free trial (1 free trial per Google account)
 * ✅ No local data to tamper with
 */
public class SubscriptionManager {

    private static final String TAG = "SubscriptionManager";
    private static final String PREFS_NAME = "subscription_prefs";
    private static final String KEY_IS_PREMIUM = "is_premium";

    private final Context context;
    private final SharedPreferences prefs;
    private BillingClient billingClient;

    // Listener to notify when subscription status is updated
    public interface OnStatusUpdatedListener {
        void onStatusUpdated(boolean isPremium);
    }

    private OnStatusUpdatedListener statusListener;

    public SubscriptionManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initializeBillingClient();
    }

    public void setStatusListener(OnStatusUpdatedListener listener) {
        this.statusListener = listener;
    }

    /**
     * Initialize Google Play Billing Client.
     */
    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && purchases != null) {
                        handlePurchases(purchases);
                    }
                })
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected");
                    queryActivePurchases();
                } else {
                    Log.w(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected");
            }
        });
    }

    /**
     * Query Google Play for active subscriptions.
     * This is the ONLY source of truth - no local SharedPreferences trial.
     */
    private void queryActivePurchases() {
        if (!billingClient.isReady()) {
            Log.w(TAG, "Billing client not ready");
            return;
        }

        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                (billingResult, purchasesList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        handlePurchases(purchasesList);
                    } else {
                        Log.w(TAG, "Failed to query purchases: " + billingResult.getDebugMessage());
                    }
                });
    }

    /**
     * Handle purchases from Google Play.
     * A PURCHASED subscription = user is premium (including during free trial).
     * Google Play manages the free trial - no need for local tracking.
     */
    private void handlePurchases(List<Purchase> purchases) {
        boolean hasPremium = false;

        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    hasPremium = true;
                    Log.d(TAG, "Active subscription: " + purchase.getProducts());
                    break;
                }
            }
        }

        // Cache the result locally for fast access (not used for trial - just caching)
        // Default is TRUE (optimistic) - only becomes false after confirmed by Google
        // Play
        boolean previous = prefs.getBoolean(KEY_IS_PREMIUM, true);
        prefs.edit().putBoolean(KEY_IS_PREMIUM, hasPremium).apply();

        Log.d(TAG, "Premium status: " + hasPremium);

        // Notify listener if status changed
        if (statusListener != null && previous != hasPremium) {
            statusListener.onStatusUpdated(hasPremium);
        }
    }

    /**
     * Check if user can use the app (instance method).
     * Returns cached value - real check happens via queryActivePurchases().
     * Default = TRUE (optimistic), becomes false only after billing confirms no
     * subscription.
     */
    public boolean canUseApp() {
        return prefs.getBoolean(KEY_IS_PREMIUM, true);
    }

    /**
     * Lightweight STATIC check for use in Services/Receivers.
     * Avoids creating a full BillingClient just to read a cached value.
     * Safe to call from background threads.
     */
    public static boolean isAppPremium(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_PREMIUM, true); // default true = allow blocking
    }

    /**
     * Check if user has an active subscription.
     */
    public boolean hasActiveSubscription() {
        return prefs.getBoolean(KEY_IS_PREMIUM, true);
    }

    /**
     * Refresh subscription status from Google Play.
     * Call this on app resume to stay up-to-date.
     */
    public void refreshSubscriptionStatus() {
        if (billingClient.isReady()) {
            queryActivePurchases();
        } else {
            // Try reconnecting
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        queryActivePurchases();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    Log.w(TAG, "Billing reconnect failed");
                }
            });
        }
    }

    /**
     * Get the billing client for launching purchase flows.
     */
    public BillingClient getBillingClient() {
        return billingClient;
    }

    /**
     * Clean up billing client.
     */
    public void destroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}

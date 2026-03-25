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
 * IMPORTANT: Call connect() after setting all listeners to avoid race conditions.
 */
public class SubscriptionManager {

    private static final String TAG = "SubscriptionManager";
    private static final String PREFS_NAME = "subscription_prefs";
    private static final String KEY_IS_PREMIUM = "is_premium";

    private final Context context;
    private final SharedPreferences prefs;
    private BillingClient billingClient;

    public interface OnStatusUpdatedListener {
        void onStatusUpdated(boolean isPremium);
    }

    public interface OnBillingReadyListener {
        void onReady();
        void onFailed(String debugMessage);
    }

    private OnStatusUpdatedListener statusListener;
    private OnBillingReadyListener billingReadyListener;
    // Skip the status listener on the very first cold-start check to avoid
    // opening the paywall before MainActivity has rendered.
    private boolean isInitialCheck = true;

    public SubscriptionManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        buildBillingClient();
        // Do NOT call startConnection here — call connect() after setting listeners.
    }

    public void setStatusListener(OnStatusUpdatedListener listener) {
        this.statusListener = listener;
    }

    public void setBillingReadyListener(OnBillingReadyListener listener) {
        this.billingReadyListener = listener;
    }

    /**
     * Set the ready listener and start the connection in one atomic call.
     * Always use this in SubscriptionActivity so the listener is registered
     * BEFORE startConnection fires — no race condition possible.
     */
    public void connect(OnBillingReadyListener listener) {
        this.billingReadyListener = listener;
        startConnection();
    }

    /**
     * Start connection without a ready listener (used by MainActivity which
     * only cares about status updates, not when billing is "ready").
     */
    public void connect() {
        startConnection();
    }

    private void buildBillingClient() {
        billingClient = BillingClient.newBuilder(context)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && purchases != null) {
                        handlePurchases(purchases);
                    }
                })
                .enablePendingPurchases()
                .build();
    }

    private void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected");
                    queryActivePurchases();
                    if (billingReadyListener != null) {
                        billingReadyListener.onReady();
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                    if (billingReadyListener != null) {
                        billingReadyListener.onFailed(billingResult.getDebugMessage());
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected");
            }
        });
    }

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

        boolean previous = prefs.getBoolean(KEY_IS_PREMIUM, true);
        prefs.edit().putBoolean(KEY_IS_PREMIUM, hasPremium).apply();
        Log.d(TAG, "Premium status: " + hasPremium + " (initialCheck=" + isInitialCheck + ")");

        // Skip the listener on the very first cold-start check so we don't
        // open the paywall before MainActivity has drawn its UI.
        if (isInitialCheck) {
            isInitialCheck = false;
            return;
        }

        if (statusListener != null && previous != hasPremium) {
            statusListener.onStatusUpdated(hasPremium);
        }
    }

    public boolean canUseApp() {
        return prefs.getBoolean(KEY_IS_PREMIUM, true);
    }

    public static boolean isAppPremium(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_PREMIUM, true);
    }

    public boolean hasActiveSubscription() {
        return prefs.getBoolean(KEY_IS_PREMIUM, true);
    }

    public void refreshSubscriptionStatus() {
        if (billingClient.isReady()) {
            queryActivePurchases();
        } else {
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

    public BillingClient getBillingClient() {
        return billingClient;
    }

    public void destroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}

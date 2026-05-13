package com.floventy.blockcalls.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.floventy.blockcalls.R;
import com.floventy.blockcalls.subscription.SubscriptionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying subscription options and handling purchases.
 *
 * Trial eligibility is determined dynamically from Google Play:
 * - If the user's Google account has never subscribed, Play returns a FREE pricing phase.
 * - If the trial was already used, Play returns only the base (paid) offer.
 * - The app picks the best offer token automatically (trial > base).
 */
public class SubscriptionActivity extends AppCompatActivity {

    private static final String TAG = "SubscriptionActivity";

    // Product IDs — must match exactly what is configured in Play Console.
    private static final String MONTHLY_SUB = "monthly_subscription";
    private static final String SIXMONTH_SUB = "sixmonth_subscription";
    private static final String YEARLY_SUB = "yearly_subscription";

    private SubscriptionManager subscriptionManager;

    // ProductDetails from Play — null until billing responds.
    private ProductDetails monthlyProduct;
    private ProductDetails sixMonthProduct;
    private ProductDetails yearlyProduct;

    // Best offer token per product (trial token if available, otherwise base token).
    private String monthlyOfferToken;
    private String sixMonthOfferToken;
    private String yearlyOfferToken;

    private ProgressBar progressBar;
    private View contentLayout;
    private TextView tvTrialInfo;
    private TextView tvRestorePurchase;
    private MaterialCardView cardMonthly, cardSixMonth, cardYearly;
    private Button btnMonthly, btnSixMonth, btnYearly;
    private TextView tvPriceMonthly, tvPriceSixMonth, tvPriceYearly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.subscription_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();

        subscriptionManager = new SubscriptionManager(this);

        // Close this screen when subscription becomes active (purchase completed or restored).
        subscriptionManager.setStatusListener(isPremium -> {
            if (isPremium) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.restore_success, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
        });

        // Connect to billing and load product details + real prices.
        subscriptionManager.connect(new SubscriptionManager.OnBillingReadyListener() {
            @Override
            public void onReady() {
                runOnUiThread(() -> loadProductDetails());
            }

            @Override
            public void onFailed(String debugMessage) {
                Log.w(TAG, "Billing unavailable: " + debugMessage);
                runOnUiThread(() -> {
                    tvTrialInfo.setText(R.string.error_product_not_available);
                    tvTrialInfo.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);
        tvTrialInfo = findViewById(R.id.tvTrialInfo);
        tvRestorePurchase = findViewById(R.id.tvRestorePurchase);

        cardMonthly = findViewById(R.id.cardMonthly);
        cardSixMonth = findViewById(R.id.cardSixMonth);
        cardYearly = findViewById(R.id.cardYearly);

        btnMonthly = findViewById(R.id.btnMonthly);
        btnSixMonth = findViewById(R.id.btnSixMonth);
        btnYearly = findViewById(R.id.btnYearly);

        tvPriceMonthly = findViewById(R.id.tvPriceMonthly);
        tvPriceSixMonth = findViewById(R.id.tvPriceSixMonth);
        tvPriceYearly = findViewById(R.id.tvPriceYearly);

        btnMonthly.setOnClickListener(v -> launchPurchaseFlow(monthlyProduct, monthlyOfferToken));
        btnSixMonth.setOnClickListener(v -> launchPurchaseFlow(sixMonthProduct, sixMonthOfferToken));
        btnYearly.setOnClickListener(v -> launchPurchaseFlow(yearlyProduct, yearlyOfferToken));

        tvRestorePurchase.setOnClickListener(v -> restorePurchase());
    }

    // ─── Product loading ──────────────────────────────────────────────────────

    private void loadProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(MONTHLY_SUB).setProductType(BillingClient.ProductType.SUBS).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SIXMONTH_SUB).setProductType(BillingClient.ProductType.SUBS).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(YEARLY_SUB).setProductType(BillingClient.ProductType.SUBS).build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList).build();

        subscriptionManager.getBillingClient().queryProductDetailsAsync(params,
                (billingResult, productDetailsList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && productDetailsList != null && !productDetailsList.isEmpty()) {

                        boolean anyTrialEligible = false;

                        for (ProductDetails product : productDetailsList) {
                            String productId = product.getProductId();
                            boolean hasTrial = hasFreeTrialOffer(product);
                            if (hasTrial) anyTrialEligible = true;

                            String offerToken = findBestOfferToken(product);

                            if (MONTHLY_SUB.equals(productId)) {
                                monthlyProduct = product;
                                monthlyOfferToken = offerToken;
                                runOnUiThread(() -> updatePriceUI(tvPriceMonthly, btnMonthly, product));
                            } else if (SIXMONTH_SUB.equals(productId)) {
                                sixMonthProduct = product;
                                sixMonthOfferToken = offerToken;
                                runOnUiThread(() -> updatePriceUI(tvPriceSixMonth, btnSixMonth, product));
                            } else if (YEARLY_SUB.equals(productId)) {
                                yearlyProduct = product;
                                yearlyOfferToken = offerToken;
                                runOnUiThread(() -> updatePriceUI(tvPriceYearly, btnYearly, product));
                            }
                        }

                        final boolean trialAvailable = anyTrialEligible;
                        runOnUiThread(() -> {
                            tvTrialInfo.setText(trialAvailable
                                    ? R.string.trial_eligible
                                    : R.string.trial_not_eligible);
                            tvTrialInfo.setVisibility(View.VISIBLE);
                        });

                    } else {
                        Log.w(TAG, "Could not fetch products: " + billingResult.getDebugMessage());
                        runOnUiThread(() -> {
                            tvTrialInfo.setText(R.string.error_product_not_available);
                            tvTrialInfo.setVisibility(View.VISIBLE);
                        });
                    }
                });
    }

    // ─── Offer helpers ────────────────────────────────────────────────────────

    /**
     * Returns true if the product has at least one offer with a FREE pricing phase.
     * Google Play only returns trial offers when the account is actually eligible.
     */
    private boolean hasFreeTrialOffer(ProductDetails product) {
        if (product.getSubscriptionOfferDetails() == null) return false;
        for (ProductDetails.SubscriptionOfferDetails offer : product.getSubscriptionOfferDetails()) {
            for (ProductDetails.PricingPhase phase : offer.getPricingPhases().getPricingPhaseList()) {
                if (phase.getPriceAmountMicros() == 0) return true;
            }
        }
        return false;
    }

    /**
     * Selects the best offer token: trial (FREE phase) > base plan.
     */
    private String findBestOfferToken(ProductDetails product) {
        if (product.getSubscriptionOfferDetails() == null
                || product.getSubscriptionOfferDetails().isEmpty()) return null;

        for (ProductDetails.SubscriptionOfferDetails offer : product.getSubscriptionOfferDetails()) {
            for (ProductDetails.PricingPhase phase : offer.getPricingPhases().getPricingPhaseList()) {
                if (phase.getPriceAmountMicros() == 0) {
                    Log.d(TAG, "Trial offer selected for: " + product.getProductId());
                    return offer.getOfferToken();
                }
            }
        }

        Log.d(TAG, "Base offer selected for: " + product.getProductId());
        return product.getSubscriptionOfferDetails().get(0).getOfferToken();
    }

    /** Returns the recurring (final) pricing phase — what the user pays after any trial. */
    private ProductDetails.PricingPhase getRecurringPhase(ProductDetails.SubscriptionOfferDetails offer) {
        List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
        return phases.get(phases.size() - 1);
    }

    // ─── UI updates ───────────────────────────────────────────────────────────

    private void updatePriceUI(TextView priceView, Button button, ProductDetails product) {
        if (product.getSubscriptionOfferDetails() == null
                || product.getSubscriptionOfferDetails().isEmpty()) return;

        boolean hasTrial = hasFreeTrialOffer(product);

        ProductDetails.SubscriptionOfferDetails selectedOffer = null;
        for (ProductDetails.SubscriptionOfferDetails offer : product.getSubscriptionOfferDetails()) {
            for (ProductDetails.PricingPhase phase : offer.getPricingPhases().getPricingPhaseList()) {
                if (phase.getPriceAmountMicros() == 0) {
                    selectedOffer = offer;
                    break;
                }
            }
            if (selectedOffer != null) break;
        }
        if (selectedOffer == null) selectedOffer = product.getSubscriptionOfferDetails().get(0);

        ProductDetails.PricingPhase recurringPhase = getRecurringPhase(selectedOffer);

        if (hasTrial) {
            priceView.setText(getString(R.string.trial_then_price, recurringPhase.getFormattedPrice()));
            button.setText(R.string.btn_start_trial);
        } else {
            priceView.setText(recurringPhase.getFormattedPrice());
            button.setText(R.string.subscribe_now);
        }
    }

    // ─── Purchase flow ────────────────────────────────────────────────────────

    private void launchPurchaseFlow(ProductDetails product, String offerToken) {
        if (product == null || offerToken == null) {
            Toast.makeText(this, R.string.error_product_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build();

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(List.of(productDetailsParams))
                .build();

        subscriptionManager.getBillingClient().launchBillingFlow(this, billingFlowParams);
    }

    // ─── Restore purchase ─────────────────────────────────────────────────────

    private void restorePurchase() {
        tvRestorePurchase.setEnabled(false);
        tvRestorePurchase.setText(R.string.restoring_purchase);

        subscriptionManager.refreshSubscriptionStatus();

        tvRestorePurchase.postDelayed(() -> {
            tvRestorePurchase.setEnabled(true);
            tvRestorePurchase.setText(R.string.restore_purchase);
            if (!SubscriptionManager.isAppPremium(this)) {
                Toast.makeText(this, R.string.restore_not_found, Toast.LENGTH_LONG).show();
            }
        }, 3000);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Safety net: if the purchase was already processed by PurchasesUpdatedListener
        // (which fires before onResume when the Play billing dialog closes), close immediately.
        if (SubscriptionManager.isAppPremium(this)) {
            setResult(RESULT_OK);
            finish();
            return;
        }
        subscriptionManager.refreshSubscriptionStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (subscriptionManager != null) {
            subscriptionManager.destroy();
        }
    }
}

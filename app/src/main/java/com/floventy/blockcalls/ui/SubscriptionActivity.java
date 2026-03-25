package com.floventy.blockcalls.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
 */
public class SubscriptionActivity extends AppCompatActivity {

    private static final String TAG = "SubscriptionActivity";

    // Product IDs matching Google Play Console
    private static final String MONTHLY_SUB = "monthly_subscription";
    private static final String SIXMONTH_SUB = "sixmonth_subscription";
    private static final String YEARLY_SUB = "yearly_subscription";

    private SubscriptionManager subscriptionManager;
    private ProductDetails monthlyProduct;
    private ProductDetails sixMonthProduct;
    private ProductDetails yearlyProduct;

    private ProgressBar progressBar;
    private View contentLayout;
    private TextView tvTrialInfo;
    private MaterialCardView cardMonthly, cardSixMonth, cardYearly;
    private Button btnMonthly, btnSixMonth, btnYearly;
    private TextView tvPriceMonthly, tvPriceSixMonth, tvPriceYearly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.subscription_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();

        subscriptionManager = new SubscriptionManager(this);

        // Show trial info
        tvTrialInfo.setText(R.string.trial_google_managed);
        tvTrialInfo.setVisibility(View.VISIBLE);

        // Close this screen when subscription becomes active
        subscriptionManager.setStatusListener(isPremium -> {
            if (isPremium) {
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            }
        });

        // Content is already visible from XML — connect billing in background to update prices
        subscriptionManager.connect(new SubscriptionManager.OnBillingReadyListener() {
            @Override
            public void onReady() {
                runOnUiThread(() -> loadProductPrices());
            }

            @Override
            public void onFailed(String debugMessage) {
                // No-op: prices already shown from XML defaults, billing just isn't available
                Log.w(TAG, "Billing unavailable, using default prices: " + debugMessage);
            }
        });
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);
        tvTrialInfo = findViewById(R.id.tvTrialInfo);

        cardMonthly = findViewById(R.id.cardMonthly);
        cardSixMonth = findViewById(R.id.cardSixMonth);
        cardYearly = findViewById(R.id.cardYearly);

        btnMonthly = findViewById(R.id.btnMonthly);
        btnSixMonth = findViewById(R.id.btnSixMonth);
        btnYearly = findViewById(R.id.btnYearly);

        tvPriceMonthly = findViewById(R.id.tvPriceMonthly);
        tvPriceSixMonth = findViewById(R.id.tvPriceSixMonth);
        tvPriceYearly = findViewById(R.id.tvPriceYearly);

        btnMonthly.setOnClickListener(v -> launchPurchaseFlow(monthlyProduct));
        btnSixMonth.setOnClickListener(v -> launchPurchaseFlow(sixMonthProduct));
        btnYearly.setOnClickListener(v -> launchPurchaseFlow(yearlyProduct));
    }

    /**
     * Fetches real prices from Play Console and updates UI.
     * Content layout is already visible — this just improves price display.
     */
    private void loadProductPrices() {
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
                            && productDetailsList != null) {
                        for (ProductDetails product : productDetailsList) {
                            String productId = product.getProductId();
                            if (MONTHLY_SUB.equals(productId)) {
                                monthlyProduct = product;
                                updatePriceUI(tvPriceMonthly, product);
                            } else if (SIXMONTH_SUB.equals(productId)) {
                                sixMonthProduct = product;
                                updatePriceUI(tvPriceSixMonth, product);
                            } else if (YEARLY_SUB.equals(productId)) {
                                yearlyProduct = product;
                                updatePriceUI(tvPriceYearly, product);
                            }
                        }
                    } else {
                        Log.w(TAG, "Could not fetch prices from Play Console: " + billingResult.getDebugMessage());
                        // Default prices from XML are still shown — no error toast needed
                    }
                });
    }

    private void updatePriceUI(TextView priceView, ProductDetails product) {
        if (product.getSubscriptionOfferDetails() != null
                && !product.getSubscriptionOfferDetails().isEmpty()) {
            ProductDetails.SubscriptionOfferDetails offer = product.getSubscriptionOfferDetails().get(0);
            if (!offer.getPricingPhases().getPricingPhaseList().isEmpty()) {
                ProductDetails.PricingPhase phase = offer.getPricingPhases().getPricingPhaseList().get(0);
                priceView.setText(phase.getFormattedPrice());
            }
        }
    }

    private void launchPurchaseFlow(ProductDetails product) {
        if (product == null) {
            Toast.makeText(this, R.string.error_product_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        if (product.getSubscriptionOfferDetails() == null
                || product.getSubscriptionOfferDetails().isEmpty()) {
            Toast.makeText(this, R.string.error_product_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        BillingFlowParams.ProductDetailsParams productDetailsParams = BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(product)
                .setOfferToken(product.getSubscriptionOfferDetails().get(0).getOfferToken())
                .build();

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(List.of(productDetailsParams))
                .build();

        BillingClient billingClient = subscriptionManager.getBillingClient();
        billingClient.launchBillingFlow(this, billingFlowParams);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Don't allow going back without subscription - minimize instead
        moveTaskToBack(true);
        return true;
    }

    @Override
    @SuppressWarnings("MissingSuperCall")
    public void onBackPressed() {
        // Don't allow dismissing the subscription screen - minimize instead
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh subscription status when returning from purchase
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

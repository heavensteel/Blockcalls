package com.floventy.blockcalls.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.floventy.blockcalls.R;
import com.floventy.blockcalls.subscription.SubscriptionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PremiumFragment extends Fragment {

    private static final String TAG = "PremiumFragment";

    private static final String MONTHLY_SUB = "monthly_subscription";
    private static final String SIXMONTH_SUB = "sixmonth_subscription";
    private static final String YEARLY_SUB = "yearly_subscription";

    private SubscriptionManager subscriptionManager;

    private ProductDetails monthlyProduct;
    private ProductDetails sixMonthProduct;
    private ProductDetails yearlyProduct;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_subscription, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        subscriptionManager = new SubscriptionManager(requireContext());

        subscriptionManager.setStatusListener(isPremium -> {
            if (isPremium && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.restore_success, Toast.LENGTH_SHORT).show();
                });
            }
        });

        subscriptionManager.connect(new SubscriptionManager.OnBillingReadyListener() {
            @Override
            public void onReady() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> loadProductDetails());
                }
            }

            @Override
            public void onFailed(String debugMessage) {
                Log.w(TAG, "Billing unavailable: " + debugMessage);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        tvTrialInfo.setText(R.string.error_product_not_available);
                        tvTrialInfo.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progressBar);
        contentLayout = view.findViewById(R.id.contentLayout);
        tvTrialInfo = view.findViewById(R.id.tvTrialInfo);
        tvRestorePurchase = view.findViewById(R.id.tvRestorePurchase);

        cardMonthly = view.findViewById(R.id.cardMonthly);
        cardSixMonth = view.findViewById(R.id.cardSixMonth);
        cardYearly = view.findViewById(R.id.cardYearly);

        btnMonthly = view.findViewById(R.id.btnMonthly);
        btnSixMonth = view.findViewById(R.id.btnSixMonth);
        btnYearly = view.findViewById(R.id.btnYearly);

        tvPriceMonthly = view.findViewById(R.id.tvPriceMonthly);
        tvPriceSixMonth = view.findViewById(R.id.tvPriceSixMonth);
        tvPriceYearly = view.findViewById(R.id.tvPriceYearly);

        btnMonthly.setOnClickListener(v -> launchPurchaseFlow(monthlyProduct, monthlyOfferToken));
        btnSixMonth.setOnClickListener(v -> launchPurchaseFlow(sixMonthProduct, sixMonthOfferToken));
        btnYearly.setOnClickListener(v -> launchPurchaseFlow(yearlyProduct, yearlyOfferToken));

        tvRestorePurchase.setOnClickListener(v -> restorePurchase());
    }

    private void loadProductDetails() {
        if (!isAdded()) return;
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
                    if (!isAdded()) return;
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
                                requireActivity().runOnUiThread(() -> updatePriceUI(tvPriceMonthly, btnMonthly, product));
                            } else if (SIXMONTH_SUB.equals(productId)) {
                                sixMonthProduct = product;
                                sixMonthOfferToken = offerToken;
                                requireActivity().runOnUiThread(() -> updatePriceUI(tvPriceSixMonth, btnSixMonth, product));
                            } else if (YEARLY_SUB.equals(productId)) {
                                yearlyProduct = product;
                                yearlyOfferToken = offerToken;
                                requireActivity().runOnUiThread(() -> updatePriceUI(tvPriceYearly, btnYearly, product));
                            }
                        }

                        final boolean trialAvailable = anyTrialEligible;
                        requireActivity().runOnUiThread(() -> {
                            tvTrialInfo.setText(trialAvailable
                                    ? R.string.trial_eligible
                                    : R.string.trial_not_eligible);
                            tvTrialInfo.setVisibility(View.VISIBLE);
                        });

                    } else {
                        Log.w(TAG, "Could not fetch products: " + billingResult.getDebugMessage());
                        requireActivity().runOnUiThread(() -> {
                            tvTrialInfo.setText(R.string.error_product_not_available);
                            tvTrialInfo.setVisibility(View.VISIBLE);
                        });
                    }
                });
    }

    private boolean hasFreeTrialOffer(ProductDetails product) {
        if (product.getSubscriptionOfferDetails() == null) return false;
        for (ProductDetails.SubscriptionOfferDetails offer : product.getSubscriptionOfferDetails()) {
            for (ProductDetails.PricingPhase phase : offer.getPricingPhases().getPricingPhaseList()) {
                if (phase.getPriceAmountMicros() == 0) return true;
            }
        }
        return false;
    }

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

    private ProductDetails.PricingPhase getRecurringPhase(ProductDetails.SubscriptionOfferDetails offer) {
        List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
        return phases.get(phases.size() - 1);
    }

    private void updatePriceUI(TextView priceView, Button button, ProductDetails product) {
        if (!isAdded()) return;
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

    private void launchPurchaseFlow(ProductDetails product, String offerToken) {
        if (!isAdded()) return;
        if (product == null || offerToken == null) {
            Toast.makeText(requireContext(), R.string.error_product_not_available, Toast.LENGTH_SHORT).show();
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

        subscriptionManager.getBillingClient().launchBillingFlow(requireActivity(), billingFlowParams);
    }

    private void restorePurchase() {
        if (!isAdded()) return;
        tvRestorePurchase.setEnabled(false);
        tvRestorePurchase.setText(R.string.restoring_purchase);

        subscriptionManager.refreshSubscriptionStatus();

        tvRestorePurchase.postDelayed(() -> {
            if (isAdded()) {
                tvRestorePurchase.setEnabled(true);
                tvRestorePurchase.setText(R.string.restore_purchase);
                if (!SubscriptionManager.isAppPremium(requireContext())) {
                    Toast.makeText(requireContext(), R.string.restore_not_found, Toast.LENGTH_LONG).show();
                }
            }
        }, 3000);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (subscriptionManager != null) {
            subscriptionManager.refreshSubscriptionStatus();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (subscriptionManager != null) {
            subscriptionManager.destroy();
        }
    }
}

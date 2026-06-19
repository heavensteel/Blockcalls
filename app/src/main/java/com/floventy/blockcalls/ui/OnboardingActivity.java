package com.floventy.blockcalls.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.floventy.blockcalls.R;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private Button btnSkip, btnNext;
    private OnboardingAdapter adapter;
    
    private final Handler animHandler = new Handler(Looper.getMainLooper());
    private Runnable activeAnimRunnable = null;
    private Animator activeAnimator = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        layoutDots = findViewById(R.id.layoutDots);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);

        adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);

        setupDots(adapter.getItemCount());
        updateDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                
                if (position == adapter.getItemCount() - 1) {
                    btnNext.setText(R.string.onboarding_btn_start);
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText(R.string.onboarding_btn_next);
                    btnSkip.setVisibility(View.VISIBLE);
                }

                // Trigger animation for the current page
                triggerAnimationForPage(position);
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    private void setupDots(int count) {
        layoutDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics())
            );
            params.setMargins(
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()),
                    0,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()),
                    0
            );
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            layoutDots.addView(dot);
        }
    }

    private void updateDots(int activePosition) {
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
            View dot = layoutDots.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
            if (i == activePosition) {
                dot.setBackgroundResource(R.drawable.dot_active);
                params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics());
            } else {
                dot.setBackgroundResource(R.drawable.dot_inactive);
                params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            }
            dot.setLayoutParams(params);
        }
    }

    private void finishOnboarding() {
        // Save first run flag
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_shown", true).apply();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopActiveAnimations();
    }

    private void stopActiveAnimations() {
        if (activeAnimRunnable != null) {
            animHandler.removeCallbacks(activeAnimRunnable);
            activeAnimRunnable = null;
        }
        if (activeAnimator != null) {
            activeAnimator.cancel();
            activeAnimator = null;
        }
    }

    private void triggerAnimationForPage(int position) {
        stopActiveAnimations();
        
        // Find the layout container of the active page view
        // Note: ViewPager2 holds its pages inside a RecyclerView
        View pageView = null;
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        if (recyclerView != null) {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                pageView = viewHolder.itemView;
            }
        }

        if (pageView == null) {
            // Retry slightly later if the view hierarchy isn't laid out yet
            animHandler.postDelayed(() -> triggerAnimationForPage(position), 100);
            return;
        }

        FrameLayout animContainer = pageView.findViewById(R.id.animationContainer);
        if (animContainer != null) {
            animContainer.removeAllViews();
            
            switch (position) {
                case 0:
                    startPage1Animation(animContainer);
                    break;
                case 1:
                    startPage2Animation(animContainer);
                    break;
                case 2:
                    startPage3Animation(animContainer);
                    break;
            }
        }
    }

    // ─── PAGE 1: Call Screening Animation ─────────────────────────────────────

    private void startPage1Animation(FrameLayout container) {
        Context ctx = container.getContext();
        
        // 1. Create a Phone chassis CardView
        CardView phone = new CardView(ctx);
        phone.setRadius(dpToPx(16));
        phone.setCardBackgroundColor(Color.parseColor("#121212"));
        phone.setElevation(dpToPx(4));
        
        FrameLayout.LayoutParams phoneParams = new FrameLayout.LayoutParams(dpToPx(160), dpToPx(260));
        phoneParams.gravity = Gravity.CENTER;
        phone.setLayoutParams(phoneParams);
        container.addView(phone);

        // Inside Phone: Container for elements
        FrameLayout phoneScreen = new FrameLayout(ctx);
        phoneScreen.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        phone.addView(phoneScreen);

        // 1a. Call Status text
        TextView tvStatus = new TextView(ctx);
        tvStatus.setText("Gelen Arama...");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(14);
        tvStatus.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dpToPx(32);
        tvStatus.setLayoutParams(statusParams);
        phoneScreen.addView(tvStatus);

        // 1b. Spam number caller ID
        TextView tvCallerId = new TextView(ctx);
        tvCallerId.setText("Spam Arama\n0850 123 4567");
        tvCallerId.setTextColor(Color.parseColor("#FF5722"));
        tvCallerId.setTextSize(16);
        tvCallerId.setTypeface(null, Typeface.BOLD);
        tvCallerId.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams callerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        callerParams.topMargin = dpToPx(60);
        tvCallerId.setLayoutParams(callerParams);
        phoneScreen.addView(tvCallerId);

        // 1c. Blocked Badge (hidden initially)
        CardView badge = new CardView(ctx);
        badge.setRadius(dpToPx(12));
        badge.setCardBackgroundColor(Color.parseColor("#4CAF50"));
        badge.setVisibility(View.INVISIBLE);
        badge.setScaleX(0);
        badge.setScaleY(0);
        
        TextView tvBadge = new TextView(ctx);
        tvBadge.setText("ENGELLENDİ");
        tvBadge.setTextColor(Color.WHITE);
        tvBadge.setTextSize(12);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setGravity(Gravity.CENTER);
        int p = dpToPx(8);
        tvBadge.setPadding(p * 2, p, p * 2, p);
        badge.addView(tvBadge);

        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeParams.gravity = Gravity.CENTER;
        badge.setLayoutParams(badgeParams);
        phoneScreen.addView(badge);

        // 1d. Shield overlay icon (hidden initially)
        ImageView shield = new ImageView(ctx);
        shield.setImageResource(R.drawable.ic_premium_star); // Fallback to premium star as shield icon for branding
        shield.setColorFilter(Color.parseColor("#1976D2"));
        shield.setVisibility(View.INVISIBLE);
        shield.setScaleX(0);
        shield.setScaleY(0);
        FrameLayout.LayoutParams shieldParams = new FrameLayout.LayoutParams(dpToPx(56), dpToPx(56));
        shieldParams.gravity = Gravity.CENTER;
        shield.setLayoutParams(shieldParams);
        phoneScreen.addView(shield);

        // Animation Loop Runnable
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                // Reset views
                phone.setTranslationX(0);
                phone.setTranslationY(0);
                tvStatus.setText("Gelen Arama...");
                tvStatus.setTextColor(Color.WHITE);
                tvCallerId.setVisibility(View.VISIBLE);
                badge.setVisibility(View.INVISIBLE);
                badge.setScaleX(0);
                badge.setScaleY(0);
                shield.setVisibility(View.INVISIBLE);
                shield.setScaleX(0);
                shield.setScaleY(0);

                // Shake Animator (simulate ringing phone)
                ObjectAnimator shake = ObjectAnimator.ofFloat(phone, "translationX", 0, 15, -15, 15, -15, 10, -10, 5, -5, 0);
                shake.setDuration(1200);
                
                shake.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // After phone rings, shield pops up
                        shield.setVisibility(View.VISIBLE);
                        
                        AnimatorSet shieldPop = new AnimatorSet();
                        ObjectAnimator scaleX = ObjectAnimator.ofFloat(shield, "scaleX", 0f, 1.2f, 1.0f);
                        ObjectAnimator scaleY = ObjectAnimator.ofFloat(shield, "scaleY", 0f, 1.2f, 1.0f);
                        shieldPop.playTogether(scaleX, scaleY);
                        shieldPop.setDuration(500);
                        shieldPop.setInterpolator(new OvershootInterpolator());
                        
                        shieldPop.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // Shield blocks call -> status changes, badge appears
                                animHandler.postDelayed(() -> {
                                    tvStatus.setText("Engellendi");
                                    tvStatus.setTextColor(Color.parseColor("#F44336"));
                                    tvCallerId.setVisibility(View.INVISIBLE);
                                    shield.setVisibility(View.INVISIBLE);

                                    badge.setVisibility(View.VISIBLE);
                                    AnimatorSet badgePop = new AnimatorSet();
                                    ObjectAnimator bx = ObjectAnimator.ofFloat(badge, "scaleX", 0f, 1.1f, 1.0f);
                                    ObjectAnimator by = ObjectAnimator.ofFloat(badge, "scaleY", 0f, 1.1f, 1.0f);
                                    badgePop.playTogether(bx, by);
                                    badgePop.setDuration(400);
                                    badgePop.setInterpolator(new OvershootInterpolator());
                                    
                                    activeAnimator = badgePop;
                                    badgePop.start();
                                    
                                    // Loop again in 2.5 seconds
                                    activeAnimRunnable = () -> startPage1Animation(container);
                                    animHandler.postDelayed(activeAnimRunnable, 2500);
                                }, 800);
                            }
                        });
                        
                        activeAnimator = shieldPop;
                        shieldPop.start();
                    }
                });

                activeAnimator = shake;
                shake.start();
            }
        };

        animHandler.post(loop);
    }

    // ─── PAGE 2: Custom Rules Typing Animation ───────────────────────────────

    private void startPage2Animation(FrameLayout container) {
        Context ctx = container.getContext();

        // 1. Mock Rules List Container
        LinearLayout mockList = new LinearLayout(ctx);
        mockList.setOrientation(LinearLayout.VERTICAL);
        mockList.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams listParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int margin = dpToPx(16);
        listParams.setMargins(margin, margin, margin, margin);
        mockList.setLayoutParams(listParams);
        container.addView(mockList);

        // 2. Dialog input area mockup
        CardView inputCard = new CardView(ctx);
        inputCard.setRadius(dpToPx(12));
        inputCard.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        LinearLayout.LayoutParams icParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(64));
        icParams.setMargins(0, 0, 0, dpToPx(24));
        inputCard.setLayoutParams(icParams);
        mockList.addView(inputCard);

        LinearLayout inputLayout = new LinearLayout(ctx);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setGravity(Gravity.CENTER_VERTICAL);
        inputLayout.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        inputCard.addView(inputLayout);

        TextView tvInput = new TextView(ctx);
        tvInput.setText("");
        tvInput.setTextColor(Color.BLACK);
        tvInput.setTextSize(16);
        tvInput.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams tip = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvInput.setLayoutParams(tip);
        inputLayout.addView(tvInput);

        TextView tvCursor = new TextView(ctx);
        tvCursor.setText("|");
        tvCursor.setTextColor(Color.parseColor("#1976D2"));
        tvCursor.setTextSize(18);
        tvCursor.setTypeface(null, Typeface.BOLD);
        inputLayout.addView(tvCursor);

        // Cursor blinking animator
        ObjectAnimator cursorBlink = ObjectAnimator.ofFloat(tvCursor, "alpha", 1.0f, 0.0f);
        cursorBlink.setDuration(500);
        cursorBlink.setRepeatCount(ValueAnimator.INFINITE);
        cursorBlink.setRepeatMode(ValueAnimator.REVERSE);
        cursorBlink.start();
        activeAnimator = cursorBlink;

        // Rule card that drops in (initially invisible)
        CardView ruleItem = new CardView(ctx);
        ruleItem.setRadius(dpToPx(8));
        ruleItem.setCardBackgroundColor(Color.WHITE);
        ruleItem.setElevation(dpToPx(2));
        ruleItem.setVisibility(View.INVISIBLE);
        
        LinearLayout ruleLayout = new LinearLayout(ctx);
        ruleLayout.setOrientation(LinearLayout.HORIZONTAL);
        ruleLayout.setGravity(Gravity.CENTER_VERTICAL);
        ruleLayout.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        ruleItem.addView(ruleLayout);

        TextView tvRuleText = new TextView(ctx);
        tvRuleText.setText("🚫  +90850*  (Aktif)");
        tvRuleText.setTextColor(Color.BLACK);
        tvRuleText.setTextSize(14);
        tvRuleText.setTypeface(null, Typeface.BOLD);
        ruleLayout.addView(tvRuleText);

        LinearLayout.LayoutParams riParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        riParams.setMargins(dpToPx(24), 0, dpToPx(24), 0);
        ruleItem.setLayoutParams(riParams);
        mockList.addView(ruleItem);

        // Typing sequence animation
        final String textToType = "+90 0850*";
        final Handler typingHandler = new Handler(Looper.getMainLooper());
        
        class Typer implements Runnable {
            int charIndex = 0;
            @Override
            public void run() {
                if (charIndex <= textToType.length()) {
                    tvInput.setText(textToType.substring(0, charIndex));
                    charIndex++;
                    activeAnimRunnable = this;
                    typingHandler.postDelayed(this, 150);
                } else {
                    // Done typing, drop the rule card
                    typingHandler.postDelayed(() -> {
                        tvInput.setText(""); // clear input
                        
                        ruleItem.setVisibility(View.VISIBLE);
                        ruleItem.setTranslationY(-dpToPx(40));
                        ruleItem.setAlpha(0f);
                        
                        AnimatorSet dropSet = new AnimatorSet();
                        ObjectAnimator dy = ObjectAnimator.ofFloat(ruleItem, "translationY", -dpToPx(40), 0);
                        ObjectAnimator da = ObjectAnimator.ofFloat(ruleItem, "alpha", 0f, 1f);
                        dropSet.playTogether(dy, da);
                        dropSet.setDuration(400);
                        dropSet.setInterpolator(new OvershootInterpolator());
                        
                        dropSet.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // Glow green shortly to indicate success
                                ruleItem.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                                typingHandler.postDelayed(() -> {
                                    ruleItem.setCardBackgroundColor(Color.WHITE);
                                    
                                    // Loop
                                    activeAnimRunnable = () -> startPage2Animation(container);
                                    typingHandler.postDelayed(activeAnimRunnable, 2500);
                                }, 600);
                            }
                        });
                        
                        activeAnimator = dropSet;
                        dropSet.start();
                    }, 600);
                }
            }
        }

        typingHandler.postDelayed(new Typer(), 500);
    }

    // ─── PAGE 3: Ready Lists Selection Animation ──────────────────────────────

    private void startPage3Animation(FrameLayout container) {
        Context ctx = container.getContext();

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int margin = dpToPx(16);
        lParams.setMargins(margin, margin, margin, margin);
        layout.setLayoutParams(lParams);
        container.addView(layout);

        // Create 3 category items
        String[] titles = {"🏦 Türkiye Bankaları", "📦 Türkiye Kargocuları", "🚫 Şüpheli Aramalar"};
        String[] descs = {"8 kural eklenecek", "4 kural eklenecek", "120 kural eklenecek"};
        List<CardView> cards = new ArrayList<>();
        List<ImageView> checks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            CardView card = new CardView(ctx);
            card.setRadius(dpToPx(8));
            card.setCardBackgroundColor(Color.WHITE);
            card.setElevation(dpToPx(2));
            card.setAlpha(0f);
            card.setTranslationX(-dpToPx(40));

            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.HORIZONTAL);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            cell.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
            card.addView(cell);

            LinearLayout textCol = new LinearLayout(ctx);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textCol.setLayoutParams(tParams);
            cell.addView(textCol);

            TextView tvTitle = new TextView(ctx);
            tvTitle.setText(titles[i]);
            tvTitle.setTextColor(Color.BLACK);
            tvTitle.setTextSize(14);
            tvTitle.setTypeface(null, Typeface.BOLD);
            textCol.addView(tvTitle);

            TextView tvDesc = new TextView(ctx);
            tvDesc.setText(descs[i]);
            tvDesc.setTextColor(Color.parseColor("#757575"));
            tvDesc.setTextSize(11);
            textCol.addView(tvDesc);

            ImageView check = new ImageView(ctx);
            check.setImageResource(android.R.drawable.checkbox_off_background);
            check.setColorFilter(Color.parseColor("#BDBDBD"));
            check.setScaleX(1.0f);
            check.setScaleY(1.0f);
            cell.addView(check);

            checks.add(check);

            LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cParams.setMargins(0, 0, 0, dpToPx(12));
            card.setLayoutParams(cParams);
            layout.addView(card);
            cards.add(card);
        }

        // Import Button at bottom
        CardView btnImport = new CardView(ctx);
        btnImport.setRadius(dpToPx(20));
        btnImport.setCardBackgroundColor(Color.parseColor("#1976D2"));
        btnImport.setAlpha(0f);
        btnImport.setScaleX(0.8f);
        btnImport.setScaleY(0.8f);

        TextView tvBtn = new TextView(ctx);
        tvBtn.setText("Seçilenleri İçe Aktar");
        tvBtn.setTextColor(Color.WHITE);
        tvBtn.setTextSize(14);
        tvBtn.setTypeface(null, Typeface.BOLD);
        tvBtn.setGravity(Gravity.CENTER);
        int p = dpToPx(8);
        tvBtn.setPadding(p * 4, (int) (p * 1.5), p * 4, (int) (p * 1.5));
        btnImport.addView(tvBtn);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, dpToPx(12), 0, 0);
        btnImport.setLayoutParams(btnParams);
        layout.addView(btnImport);

        // Staggered Entrance Animator
        AnimatorSet entrance = new AnimatorSet();
        List<Animator> anims = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            CardView card = cards.get(i);
            ObjectAnimator tx = ObjectAnimator.ofFloat(card, "translationX", -dpToPx(40), 0);
            ObjectAnimator a = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
            tx.setStartDelay(i * 150);
            a.setStartDelay(i * 150);
            anims.add(tx);
            anims.add(a);
        }
        entrance.playTogether(anims);
        entrance.setDuration(400);
        
        entrance.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Animate checkmark checks one by one
                animHandler.postDelayed(() -> checkItem(0), 300);
            }

            private void checkItem(int index) {
                if (index >= checks.size()) {
                    // Checkboxes checked, now show import button
                    btnImport.setVisibility(View.VISIBLE);
                    AnimatorSet btnShow = new AnimatorSet();
                    ObjectAnimator ba = ObjectAnimator.ofFloat(btnImport, "alpha", 0f, 1f);
                    ObjectAnimator bx = ObjectAnimator.ofFloat(btnImport, "scaleX", 0.8f, 1f);
                    ObjectAnimator by = ObjectAnimator.ofFloat(btnImport, "scaleY", 0.8f, 1f);
                    btnShow.playTogether(ba, bx, by);
                    btnShow.setDuration(400);
                    btnShow.setInterpolator(new OvershootInterpolator());
                    btnShow.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Pulsate button on success
                            animHandler.postDelayed(() -> {
                                btnImport.setCardBackgroundColor(Color.parseColor("#4CAF50"));
                                tvBtn.setText("✓ Listeler Eklendi!");
                                
                                activeAnimRunnable = () -> startPage3Animation(container);
                                animHandler.postDelayed(activeAnimRunnable, 2500);
                            }, 800);
                        }
                    });
                    
                    activeAnimator = btnShow;
                    btnShow.start();
                    return;
                }

                ImageView check = checks.get(index);
                // Simple scale check animation
                check.setImageResource(android.R.drawable.checkbox_on_background);
                check.setColorFilter(Color.parseColor("#4CAF50"));
                AnimatorSet pop = new AnimatorSet();
                pop.playTogether(
                        ObjectAnimator.ofFloat(check, "scaleX", 1.0f, 1.4f, 1.0f),
                        ObjectAnimator.ofFloat(check, "scaleY", 1.0f, 1.4f, 1.0f)
                );
                pop.setDuration(300);
                pop.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        checkItem(index + 1);
                    }
                });
                
                activeAnimator = pop;
                pop.start();
            }
        });

        activeAnimator = entrance;
        entrance.start();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // ─── ViewPager Adapter ───────────────────────────────────────────────────

    private static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

        private final Context context;
        private final List<PageData> pages = new ArrayList<>();

        OnboardingAdapter(Context context) {
            this.context = context;
            pages.add(new PageData(
                    context.getString(R.string.onboarding_title_1),
                    context.getString(R.string.onboarding_desc_1)
            ));
            pages.add(new PageData(
                    context.getString(R.string.onboarding_title_2),
                    context.getString(R.string.onboarding_desc_2)
            ));
            pages.add(new PageData(
                    context.getString(R.string.onboarding_title_3),
                    context.getString(R.string.onboarding_desc_3)
            ));
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_onboarding_page, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PageData page = pages.get(position);
            holder.tvTitle.setText(page.title);
            holder.tvDescription.setText(page.description);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            FrameLayout animationContainer;
            TextView tvTitle, tvDescription;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                animationContainer = itemView.findViewById(R.id.animationContainer);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
            }
        }

        private static class PageData {
            final String title;
            final String description;

            PageData(String title, String description) {
                this.title = title;
                this.description = description;
            }
        }
    }
}

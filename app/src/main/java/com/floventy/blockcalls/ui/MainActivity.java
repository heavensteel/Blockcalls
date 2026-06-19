package com.floventy.blockcalls.ui;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.floventy.blockcalls.R;
import com.floventy.blockcalls.subscription.SubscriptionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main activity with bottom navigation for Rules and Blocked Calls tabs.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int ROLE_REQUEST_CODE = 101;
    private static final int SUBSCRIPTION_REQUEST_CODE = 102;

    private final RulesFragment rulesFragment = new RulesFragment();
    private final BlockedCallsFragment blockedCallsFragment = new BlockedCallsFragment();
    private final GuideFragment guideFragment = new GuideFragment();
    private final PremiumFragment premiumFragment = new PremiumFragment();
    private Fragment activeFragment;
    private SubscriptionManager subscriptionManager;
    private boolean subscriptionScreenShown = false;
    private boolean activityResumed = false;  // ensures paywall only shows after UI is visible

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Trial banner (legacy — no longer triggered since login was removed)
        int trialDaysLeft = getIntent().getIntExtra("trial_days_left", -1);
        if (trialDaysLeft == 0) {
            Toast.makeText(this, getString(R.string.trial_expired), Toast.LENGTH_LONG).show();
        } else if (trialDaysLeft > 0) {
            Toast.makeText(this, getString(R.string.trial_days_remaining, trialDaysLeft), Toast.LENGTH_LONG).show();
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // Add all fragments, hide non-active initially
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, blockedCallsFragment, "blocked_calls").hide(blockedCallsFragment)
                    .add(R.id.fragmentContainer, guideFragment, "guide").hide(guideFragment)
                    .add(R.id.fragmentContainer, premiumFragment, "premium").hide(premiumFragment)
                    .add(R.id.fragmentContainer, rulesFragment, "rules")
                    .commit();
            activeFragment = rulesFragment;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            if (item.getItemId() == R.id.nav_rules) {
                selectedFragment = rulesFragment;
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.main_title);
                }
            } else if (item.getItemId() == R.id.nav_blocked_calls) {
                selectedFragment = blockedCallsFragment;
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.blocked_calls_log);
                }
            } else if (item.getItemId() == R.id.nav_guide) {
                selectedFragment = guideFragment;
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.action_guide);
                }
            } else if (item.getItemId() == R.id.nav_premium) {
                selectedFragment = premiumFragment;
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.subscription_title);
                }
            } else {
                return false;
            }

            if (selectedFragment != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(selectedFragment)
                        .commit();
                activeFragment = selectedFragment;
            }
            invalidateOptionsMenu();
            return true;
        });

        // Initialize subscription manager and set up listener ONCE in onCreate
        subscriptionManager = new SubscriptionManager(this);
        subscriptionManager.setStatusListener(isPremium -> {
            if (!isPremium && !subscriptionScreenShown && activityResumed) {
                // Subscription expired/cancelled → show paywall (only after UI is visible)
                runOnUiThread(() -> {
                    subscriptionScreenShown = true;
                    Intent intent = new Intent(this, SubscriptionActivity.class);
                    startActivityForResult(intent, SUBSCRIPTION_REQUEST_CODE);
                });
            } else if (isPremium) {
                // Subscription restored/renewed → reset flag
                subscriptionScreenShown = false;
            }
            // Refresh toolbar icon to reflect current premium status
            runOnUiThread(this::invalidateOptionsMenu);
        });
        // Connect AFTER listener is set — no race condition possible
        subscriptionManager.connect();

        // Check permissions
        checkPermissions();

        // Initialize local dynamic safe list cache
        com.floventy.blockcalls.utils.TrustedNumbers.initialize(this);

        // Fetch dynamic Whitelist from GitHub
        new com.floventy.blockcalls.utils.TrustedListsFetcher().fetch(this, new com.floventy.blockcalls.utils.TrustedListsFetcher.Callback() {
            @Override
            public void onSuccess(java.util.List<String> prefixes, java.util.List<String> exact) {
                com.floventy.blockcalls.utils.TrustedNumbers.setDynamicLists(prefixes, exact);
            }

            @Override
            public void onError(String message) {
                android.util.Log.w("MainActivity", "Failed to fetch dynamic safe lists: " + message);
            }
        });

        // First run onboarding check
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("onboarding_shown", false)) {
            Intent intent = new Intent(this, OnboardingActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        // Refresh Play Billing status
        if (subscriptionManager != null) {
            subscriptionManager.refreshSubscriptionStatus();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SUBSCRIPTION_REQUEST_CODE) {
            // User returned from SubscriptionActivity
            // Reset the flag so we can show it again if subscription still inactive
            subscriptionScreenShown = false;
            // Re-check status immediately
            if (subscriptionManager != null) {
                subscriptionManager.refreshSubscriptionStatus();
            }
        } else if (requestCode == ROLE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    Toast.makeText(this, R.string.call_screening_active, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.call_screening_failed, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Update title based on current subscription status
        MenuItem subItem = menu.findItem(R.id.action_subscription);
        if (subItem != null && subscriptionManager != null) {
            subItem.setTitle(subscriptionManager.canUseApp()
                    ? getString(R.string.manage_subscription)
                    : getString(R.string.action_subscription));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_subscription) {
            Intent intent = new Intent(this, SubscriptionActivity.class);
            startActivityForResult(intent, SUBSCRIPTION_REQUEST_CODE);
            return true;
        } else if (item.getItemId() == R.id.action_guide) {
            Intent intent = new Intent(this, OnboardingActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_suggest_safe) {
            showSuggestSafeNumberDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (subscriptionManager != null) {
            subscriptionManager.destroy();
        }
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.READ_CONTACTS
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            checkCallScreeningRole();
        }
    }

    private void checkCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    try {
                        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
                        startActivityForResult(intent, ROLE_REQUEST_CODE);
                    } catch (Exception e) {
                        Log.w("MainActivity", "CallScreening role not available on this device", e);
                        Toast.makeText(this, R.string.call_screening_not_supported, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                checkCallScreeningRole();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showSuggestSafeNumberDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.suggest_safe_title);
        builder.setMessage(R.string.suggest_safe_desc);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        layout.setPadding(padding, padding, padding, padding);

        final com.google.android.material.textfield.TextInputEditText etNumber = new com.google.android.material.textfield.TextInputEditText(this);
        etNumber.setHint(R.string.suggest_safe_num_hint);
        etNumber.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        android.widget.LinearLayout.LayoutParams params1 = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params1.bottomMargin = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        etNumber.setLayoutParams(params1);
        layout.addView(etNumber);

        final com.google.android.material.textfield.TextInputEditText etName = new com.google.android.material.textfield.TextInputEditText(this);
        etName.setHint(R.string.suggest_safe_name_hint);
        etName.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        etName.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(etName);

        builder.setView(layout);

        builder.setPositiveButton(R.string.suggest_safe_send, (dialog, which) -> {
            String number = etNumber.getText() != null ? etNumber.getText().toString().trim() : "";
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";

            if (number.isEmpty()) {
                Toast.makeText(this, "Numara alanı boş olamaz", Toast.LENGTH_SHORT).show();
                return;
            }

            sendSafeNumberEmail(number, name);
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void sendSafeNumberEmail(String number, String name) {
        String body = "Önerilen Güvenli Numara: " + number + "\nFirma/Kurum Adı: " + name + "\n\nCihaz Modeli: " + android.os.Build.MODEL + "\nAndroid Sürümü: " + android.os.Build.VERSION.RELEASE;
        
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(android.net.Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@floventy.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.suggest_safe_email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(intent, "E-posta Uygulaması Seçin"));
        } catch (Exception e) {
            Toast.makeText(this, "E-posta uygulaması bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }
}

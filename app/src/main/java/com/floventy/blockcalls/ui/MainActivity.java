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
    private Fragment activeFragment;
    private SubscriptionManager subscriptionManager;
    private boolean subscriptionScreenShown = false;
    private boolean activityResumed = false;  // ensures paywall only shows after UI is visible

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // Add both fragments, hide blocked calls initially
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, blockedCallsFragment, "blocked_calls")
                    .hide(blockedCallsFragment)
                    .commit();
            getSupportFragmentManager().beginTransaction()
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        // Only refresh billing status - listener in onCreate handles the redirect
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
}

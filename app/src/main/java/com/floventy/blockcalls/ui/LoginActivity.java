package com.floventy.blockcalls.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.floventy.blockcalls.R;
import com.floventy.blockcalls.subscription.FirebaseSubscriptionHelper;
import com.floventy.blockcalls.subscription.SubscriptionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final int RC_SUBSCRIPTION = 1001;

    private EditText etEmail;
    private Button btnContinue;
    private MaterialButton btnGoogle;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView tvCheckingStatus;

    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        btnContinue = findViewById(R.id.btnContinue);
        btnGoogle = findViewById(R.id.btnGoogle);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        tvCheckingStatus = findViewById(R.id.tvCheckingStatus);

        setupGoogleSignIn();

        // Already logged in — re-verify silently
        String savedEmail = FirebaseSubscriptionHelper.getSavedEmail(this);
        if (savedEmail != null) {
            showCheckingMode(savedEmail);
            verifyAndProceed(savedEmail, true);
            return;
        }

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());

        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(getString(R.string.error_invalid_email));
                return;
            }
            tvError.setVisibility(View.GONE);
            FirebaseSubscriptionHelper.saveEmail(this, email);
            verifyAndProceed(email, false);
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void startGoogleSignIn() {
        // Sign out first so the account picker always appears
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String email = account.getEmail();
                if (email != null) {
                    FirebaseSubscriptionHelper.saveEmail(this, email);
                    showCheckingMode(email);
                    verifyAndProceed(email, false);
                } else {
                    showError(getString(R.string.error_google_no_email));
                }
            } catch (ApiException e) {
                showError(getString(R.string.error_google_failed));
            }
            return;
        }

        if (requestCode == RC_SUBSCRIPTION) {
            String email = FirebaseSubscriptionHelper.getSavedEmail(this);
            if (email != null) {
                showCheckingMode(email);
                verifyAndProceed(email, true);
            }
        }
    }

    private void showCheckingMode(String email) {
        etEmail.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);
        btnGoogle.setVisibility(View.GONE);
        // Hide OR divider and email section
        View orDivider = findViewById(R.id.tilEmail);
        if (orDivider != null) orDivider.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        tvCheckingStatus.setText(getString(R.string.checking_subscription, email));
        tvCheckingStatus.setVisibility(View.VISIBLE);
    }

    private void verifyAndProceed(String email, boolean useCacheOnError) {
        setLoading(true);

        FirebaseSubscriptionHelper.checkSubscription(email, new FirebaseSubscriptionHelper.SubscriptionCallback() {
            @Override
            public void onResult(boolean isActive, int trialDaysLeft) {
                FirebaseSubscriptionHelper.savePremiumStatus(LoginActivity.this, isActive);
                runOnUiThread(() -> {
                    setLoading(false);
                    if (isActive) {
                        goToMain(trialDaysLeft);
                    } else {
                        Intent intent = new Intent(LoginActivity.this, SubscriptionActivity.class);
                        intent.putExtra("from_login", true);
                        intent.putExtra("trial_expired", trialDaysLeft == 0);
                        startActivityForResult(intent, RC_SUBSCRIPTION);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (useCacheOnError && SubscriptionManager.isAppPremium(LoginActivity.this)) {
                        goToMain(-1);
                    } else {
                        showError(getString(R.string.error_network_retry));
                        // Restore the login form
                        etEmail.setVisibility(View.VISIBLE);
                        btnContinue.setVisibility(View.VISIBLE);
                        btnGoogle.setVisibility(View.VISIBLE);
                        tvCheckingStatus.setVisibility(View.GONE);
                        etEmail.setText(email);
                    }
                });
            }
        });
    }

    private void goToMain(int trialDaysLeft) {
        Intent intent = new Intent(this, MainActivity.class);
        if (trialDaysLeft >= 0) {
            intent.putExtra("trial_days_left", trialDaysLeft);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnContinue.setEnabled(!loading);
        btnGoogle.setEnabled(!loading);
        etEmail.setEnabled(!loading);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}

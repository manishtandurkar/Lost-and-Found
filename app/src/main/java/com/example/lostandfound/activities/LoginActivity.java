package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.lostandfound.R;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.viewmodels.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private MaterialButton btnGoogleSignIn;
    private ProgressBar progressBar;
    private TextView tvError;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleSignInResult(task);
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        observeViewModel();
    }

    private void initViews() {
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);

        btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
    }

    private void observeViewModel() {
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                progressBar.setVisibility(View.GONE);
                btnGoogleSignIn.setEnabled(true);
                navigateToMain();
            }
        });

        authViewModel.getErrorLiveData().observe(this, error -> {
            progressBar.setVisibility(View.GONE);
            btnGoogleSignIn.setEnabled(true);

            if ("domain_error".equals(error)) {
                showError(getString(R.string.login_error_domain));
            } else if (error != null) {
                showError(getString(R.string.login_error_generic));
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                btnGoogleSignIn.setEnabled(false);
                tvError.setVisibility(View.GONE);
            }
        });
    }

    private void startGoogleSignIn() {
        Intent signInIntent = authViewModel.getGoogleSignInClient().getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String email = account.getEmail();

            // Client-side domain check
            if (email == null || !email.endsWith(Constants.COLLEGE_EMAIL_DOMAIN)) {
                showError(getString(R.string.login_error_domain));
                // Sign out of Google to allow a different account
                authViewModel.getGoogleSignInClient().signOut();
                return;
            }

            String idToken = account.getIdToken();
            if (idToken != null) {
                authViewModel.signInWithGoogle(idToken);
            } else {
                showError(getString(R.string.login_error_generic));
            }
        } catch (ApiException e) {
            showError(getString(R.string.login_error_generic));
            progressBar.setVisibility(View.GONE);
            btnGoogleSignIn.setEnabled(true);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

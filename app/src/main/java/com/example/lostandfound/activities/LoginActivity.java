package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lostandfound.MainActivity;
import com.example.lostandfound.R;
import com.example.lostandfound.models.User;
import com.example.lostandfound.repository.UserRepository;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private ProgressBar progressBar;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        userRepository = new UserRepository();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        progressBar = findViewById(R.id.progressBarLogin);
        rootView = findViewById(R.id.rootLoginLayout);

        MaterialButton btnSignIn = findViewById(R.id.btnGoogleSignIn);
        btnSignIn.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, Constants.RC_SIGN_IN);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                handleSignInResult(account);
            } catch (ApiException e) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(rootView, "Sign-in failed (code " + e.getStatusCode() + "): " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void handleSignInResult(GoogleSignInAccount account) {
        String email = account.getEmail();
        if (email == null || !email.endsWith("@" + Constants.COLLEGE_DOMAIN)) {
            progressBar.setVisibility(View.GONE);
            firebaseAuth.signOut();
            googleSignInClient.signOut();
            Snackbar.make(rootView,
                    "Only " + Constants.COLLEGE_DOMAIN + " accounts are allowed.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = firebaseAuth.getCurrentUser();
                        if (fbUser != null) {
                            saveUserToFirebase(fbUser, account);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Snackbar.make(rootView, "Authentication failed.", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirebase(FirebaseUser fbUser, GoogleSignInAccount account) {
        User user = new User(
                fbUser.getUid(),
                account.getDisplayName(),
                account.getEmail(),
                account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "",
                Constants.COLLEGE_DOMAIN,
                ""
        );

        userRepository.createOrUpdateUser(user, new UserRepository.Callback() {
            @Override
            public void onSuccess() {
                sessionManager.saveSession(
                        fbUser.getUid(),
                        account.getDisplayName(),
                        account.getEmail(),
                        account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : ""
                );
                progressBar.setVisibility(View.GONE);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(rootView, "Failed to save profile: " + message, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}

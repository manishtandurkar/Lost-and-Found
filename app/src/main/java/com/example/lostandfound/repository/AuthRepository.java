package com.example.lostandfound.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.lostandfound.models.User;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AuthRepository {

    private static final String TAG = "AuthRepository";

    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference usersRef;
    private final SessionManager sessionManager;
    private final GoogleSignInClient googleSignInClient;

    public AuthRepository(Context context) {
        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS);
        sessionManager = new SessionManager(context);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(com.example.lostandfound.R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public GoogleSignInClient getGoogleSignInClient() {
        return googleSignInClient;
    }

    /**
     * Authenticate with Firebase using a Google Sign-In credential.
     * Returns the result via MutableLiveData.
     */
    public void firebaseAuthWithGoogle(String idToken,
                                        MutableLiveData<FirebaseUser> userLiveData,
                                        MutableLiveData<String> errorLiveData) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        String email = firebaseUser.getEmail();
                        if (email != null && email.endsWith(Constants.COLLEGE_EMAIL_DOMAIN)) {
                            saveOrUpdateUser(firebaseUser, userLiveData, errorLiveData);
                        } else {
                            // Domain check failed — sign out immediately
                            firebaseAuth.signOut();
                            googleSignInClient.signOut();
                            errorLiveData.postValue("domain_error");
                        }
                    } else {
                        errorLiveData.postValue("generic_error");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase auth failed", e);
                    errorLiveData.postValue(e.getMessage());
                });
    }

    /**
     * Save new user or update existing user in Firebase, then save session.
     */
    private void saveOrUpdateUser(FirebaseUser firebaseUser,
                                   MutableLiveData<FirebaseUser> userLiveData,
                                   MutableLiveData<String> errorLiveData) {
        String userId = firebaseUser.getUid();
        String name = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String photoUrl = firebaseUser.getPhotoUrl() != null
                ? firebaseUser.getPhotoUrl().toString() : "";

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // First login — create user profile
                    User newUser = new User(userId, name, email, photoUrl);
                    usersRef.child(userId).setValue(newUser)
                            .addOnSuccessListener(unused -> {
                                sessionManager.saveSession(userId, name, email, photoUrl);
                                userLiveData.postValue(firebaseUser);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create user profile", e);
                                // Still let the user in even if profile creation fails
                                sessionManager.saveSession(userId, name, email, photoUrl);
                                userLiveData.postValue(firebaseUser);
                            });
                } else {
                    // Returning user — update photo URL if changed
                    usersRef.child(userId).child("profilePhotoUrl").setValue(photoUrl);
                    sessionManager.saveSession(userId, name, email, photoUrl);
                    userLiveData.postValue(firebaseUser);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error checking user", error.toException());
                // Still save session and continue
                sessionManager.saveSession(userId, name, email, photoUrl);
                userLiveData.postValue(firebaseUser);
            }
        });
    }

    /**
     * Sign out from Firebase and Google, clear session.
     */
    public void signOut(MutableLiveData<Boolean> signOutLiveData) {
        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            sessionManager.clearSession();
            signOutLiveData.postValue(true);
        });
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }
}

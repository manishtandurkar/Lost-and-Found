package com.example.lostandfound.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.lostandfound.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> signOutLiveData = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
    }

    public com.google.android.gms.auth.api.signin.GoogleSignInClient getGoogleSignInClient() {
        return authRepository.getGoogleSignInClient();
    }

    /**
     * Called after Google Sign-In succeeds — authenticate with Firebase.
     */
    public void signInWithGoogle(String idToken) {
        isLoading.setValue(true);
        authRepository.firebaseAuthWithGoogle(idToken, userLiveData, errorLiveData);
    }

    /**
     * Sign out and clear session.
     */
    public void signOut() {
        authRepository.signOut(signOutLiveData);
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public MutableLiveData<FirebaseUser> getUserLiveData() { return userLiveData; }
    public MutableLiveData<String> getErrorLiveData() { return errorLiveData; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<Boolean> getSignOutLiveData() { return signOutLiveData; }
}

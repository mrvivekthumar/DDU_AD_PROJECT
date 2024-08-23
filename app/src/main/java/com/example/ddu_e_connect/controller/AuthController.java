package com.example.ddu_e_connect.controller;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthController {
    private FirebaseAuth firebaseAuth;

    public AuthController() {

        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public Task<AuthResult> register(String email, String password, OnAuthCompleteListener onAuthCompleteListener) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    user.sendEmailVerification().addOnCompleteListener(emailVerificationTask -> {
                        if (emailVerificationTask.isSuccessful()) {
                            onAuthCompleteListener.onSuccess(user);
                        } else {
                            onAuthCompleteListener.onFailure("Failed to send verification email: " + emailVerificationTask.getException().getMessage());
                        }
                    });
                } else {
                    onAuthCompleteListener.onFailure("Failed to get user information.");
                }
            } else {
                onAuthCompleteListener.onFailure("Registration failed: " + task.getException().getMessage());
            }
        });
    }

    public Task<FirebaseUser> signIn(String email, String password, OnAuthCompleteListener onAuthCompleteListener) {
        return firebaseAuth.signInWithEmailAndPassword(email, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        // Sign-in failed
                        throw task.getException();
                    }
                    // Sign-in successful, reload user info
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        return user.reload().continueWith(reloadTask -> {
                            if (!reloadTask.isSuccessful()) {
                                throw reloadTask.getException();
                            }
                            return user;
                        });
                    } else {
                        throw new RuntimeException("User not found.");
                    }
                }).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Notify success
                        FirebaseUser user = task.getResult();
                        onAuthCompleteListener.onSuccess(user);
                    } else {
                        // Notify failure
                        onAuthCompleteListener.onFailure("Sign-in failed: " + task.getException().getMessage());
                    }
                });
    }

    public Task<Void> sendPasswordResetEmail(String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    public interface OnAuthCompleteListener {
        void onSuccess(FirebaseUser user);

        void onFailure(String errorMessage);
    }
}

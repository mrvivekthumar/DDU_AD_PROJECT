package com.example.ddu_e_connect.controller;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthController {
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AuthController() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public interface RoleCallback {
        void onRoleFetched(String role);
        void onError(String errorMessage);
    }

    public Task<AuthResult> register(String email, String password, OnAuthCompleteListener onAuthCompleteListener) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // Create user document with no_role by default
                    DocumentReference userDoc = db.collection("users").document(user.getUid());
                    userDoc.set(new UserRole("no_role")).addOnCompleteListener(docTask -> {
                        if (docTask.isSuccessful()) {
                            user.sendEmailVerification().addOnCompleteListener(emailVerificationTask -> {
                                if (emailVerificationTask.isSuccessful()) {
                                    onAuthCompleteListener.onSuccess(user);
                                } else {
                                    onAuthCompleteListener.onFailure("Failed to send verification email: " + emailVerificationTask.getException().getMessage());
                                }
                            });
                        } else {
                            onAuthCompleteListener.onFailure("Failed to create user document: " + docTask.getException().getMessage());
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
                        throw task.getException();
                    }
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
                        FirebaseUser user = task.getResult();
                        fetchUserRole(user.getUid(), new RoleCallback() {
                            @Override
                            public void onRoleFetched(String role) {
                                // Role fetched successfully
                                onAuthCompleteListener.onSuccess(user);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // Role fetch failed
                                onAuthCompleteListener.onFailure(errorMessage);
                            }
                        });
                    } else {
                        onAuthCompleteListener.onFailure("Sign-in failed: " + task.getException().getMessage());
                    }
                });
    }

    public Task<Void> sendPasswordResetEmail(String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    public void fetchUserRole(String userId, RoleCallback callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            callback.onRoleFetched(role);
                        } else {
                            callback.onError("Role field is missing in the document for userId: " + userId);
                        }
                    } else {
                        callback.onError("Document does not exist for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> callback.onError("Failed to fetch user role: " + e.getMessage()));
    }

    public interface OnAuthCompleteListener {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }

    public static class UserRole {
        private String role;

        public UserRole() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public UserRole(String role) {
            this.role = role;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}

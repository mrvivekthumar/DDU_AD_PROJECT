package com.example.ddu_e_connect.data.repository;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository class for Google Authentication operations
 * Handles Google Sign-In and user data management
 */
public class GoogleAuthRepository {
    private static final String TAG = "GoogleAuthRepository";
    private static final String USERS_COLLECTION = "users";
    private static final String DEFAULT_ROLE = "student";

    // Replace with your actual web client ID from Firebase Console
    private static final String WEB_CLIENT_ID = "567048253235-eamihipa3vc015jh0fdo3qmspl33r36n.apps.googleusercontent.com";

    private GoogleSignInClient googleSignInClient;
    private FirebaseFirestore db;
    private Context context;

    public GoogleAuthRepository(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        initializeGoogleSignIn();
    }

    /**
     * Initialize Google Sign-In configuration
     */
    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .requestProfile()
                .build();

        googleSignInClient = GoogleSignIn.getClient(context, gso);
        Log.d(TAG, "Google Sign-In initialized");
    }

    // Interfaces for callbacks
    public interface AuthCallback {
        void onSuccess(UserProfile userProfile);
        void onFailure(String errorMessage);
    }

    public interface RoleCallback {
        void onRoleFetched(String role);
        void onError(String errorMessage);
    }

    /**
     * Get Google Sign-In intent
     * @return Intent for Google Sign-In
     */
    public Intent getSignInIntent() {
        Log.d(TAG, "Creating Google Sign-In intent");
        return googleSignInClient.getSignInIntent();
    }

    /**
     * Handle Google Sign-In result
     * @param data Intent data from sign-in result
     * @param callback Callback for authentication result
     */
    public void handleSignInResult(Intent data, AuthCallback callback) {
        if (data == null) {
            Log.e(TAG, "Sign-in data is null");
            callback.onFailure("Sign-in was cancelled");
            return;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                Log.d(TAG, "Google Sign-In successful for: " + account.getEmail());
                handleSuccessfulSignIn(account, callback);
            } else {
                Log.e(TAG, "Google Sign-In account is null");
                callback.onFailure("Failed to get account information");
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In failed", e);
            String errorMessage = getGoogleSignInErrorMessage(e.getStatusCode());
            callback.onFailure(errorMessage);
        }
    }

    /**
     * Handle successful Google Sign-In
     * @param account Google account information
     * @param callback Callback for authentication result
     */
    private void handleSuccessfulSignIn(GoogleSignInAccount account, AuthCallback callback) {
        // Validate institutional email if required
        if (!isValidInstitutionalEmail(account.getEmail())) {
            Log.w(TAG, "Non-institutional email attempted: " + account.getEmail());
            signOut(); // Sign out the user
            callback.onFailure("Please use your institutional email (@ddu.ac.in)");
            return;
        }

        UserProfile userProfile = createUserProfile(account);

        // Check if user exists in Firestore, create if not
        checkAndCreateUser(userProfile, callback);
    }

    /**
     * Check if user exists in Firestore and create if necessary
     * @param userProfile User profile from Google
     * @param callback Callback for authentication result
     */
    private void checkAndCreateUser(UserProfile userProfile, AuthCallback callback) {
        Log.d(TAG, "Checking user in Firestore: " + userProfile.getUserId());

        DocumentReference userDoc = db.collection(USERS_COLLECTION).document(userProfile.getUserId());

        userDoc.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User exists, fetch their role
                        String role = documentSnapshot.getString("role");
                        userProfile.setRole(role != null ? role : DEFAULT_ROLE);
                        Log.d(TAG, "Existing user found with role: " + userProfile.getRole());
                        callback.onSuccess(userProfile);
                    } else {
                        // New user, create document
                        Log.d(TAG, "New user, creating Firestore document");
                        createUserDocument(userProfile, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check user in Firestore", e);
                    callback.onFailure("Failed to verify user: " + e.getMessage());
                });
    }

    /**
     * Create new user document in Firestore
     * @param userProfile User profile to save
     * @param callback Callback for authentication result
     */
    private void createUserDocument(UserProfile userProfile, AuthCallback callback) {
        DocumentReference userDoc = db.collection(USERS_COLLECTION).document(userProfile.getUserId());

        UserDocument userData = new UserDocument(
                userProfile.getName(),
                userProfile.getEmail(),
                userProfile.getPhotoUrl(),
                DEFAULT_ROLE,
                System.currentTimeMillis()
        );

        userDoc.set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created successfully");
                    userProfile.setRole(DEFAULT_ROLE);
                    callback.onSuccess(userProfile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user document", e);
                    callback.onFailure("Failed to create user profile: " + e.getMessage());
                });
    }

    /**
     * Get currently signed-in user
     * @return GoogleSignInAccount or null if not signed in
     */
    public GoogleSignInAccount getCurrentUser() {
        return GoogleSignIn.getLastSignedInAccount(context);
    }

    /**
     * Check if user is currently signed in
     * @return true if user is signed in, false otherwise
     */
    public boolean isUserSignedIn() {
        return getCurrentUser() != null;
    }

    /**
     * Fetch user role from Firestore
     * @param userId User's unique ID
     * @param callback Callback for role fetch result
     */
    public void fetchUserRole(String userId, RoleCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("User ID cannot be empty");
            return;
        }

        Log.d(TAG, "Fetching role for user: " + userId);

        db.collection(USERS_COLLECTION).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null && !role.trim().isEmpty()) {
                            Log.d(TAG, "User role fetched: " + role);
                            callback.onRoleFetched(role);
                        } else {
                            Log.w(TAG, "Role not found, using default");
                            callback.onRoleFetched(DEFAULT_ROLE);
                        }
                    } else {
                        Log.w(TAG, "User document not found");
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user role", e);
                    callback.onError("Failed to fetch user role: " + e.getMessage());
                });
    }

    /**
     * Update user role in Firestore
     * @param userId User's unique ID
     * @param newRole New role to assign
     * @param callback Callback for update result
     */
    public void updateUserRole(String userId, String newRole, RoleCallback callback) {
        if (userId == null || userId.trim().isEmpty() || newRole == null || newRole.trim().isEmpty()) {
            callback.onError("User ID and role cannot be empty");
            return;
        }

        Log.d(TAG, "Updating role for user: " + userId + " to: " + newRole);

        db.collection(USERS_COLLECTION).document(userId)
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User role updated successfully");
                    callback.onRoleFetched(newRole);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user role", e);
                    callback.onError("Failed to update user role: " + e.getMessage());
                });
    }

    /**
     * Sign out current user
     * @param callback Callback for sign-out result
     */
    public void signOut(Runnable callback) {
        Log.d(TAG, "Signing out user");

        googleSignInClient.signOut()
                .addOnCompleteListener((Activity) context, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User signed out successfully");
                        callback.run();
                    } else {
                        Log.e(TAG, "Failed to sign out", task.getException());
                        callback.run(); // Still call callback even if sign out fails
                    }
                });
    }

    /**
     * Silent sign out without callback
     */
    public void signOut() {
        Log.d(TAG, "Silent sign out");
        googleSignInClient.signOut();
    }

    // Private helper methods

    /**
     * Validate if email is from institution (optional)
     * Remove this method if you want to allow any Google account
     */
    private boolean isValidInstitutionalEmail(String email) {
        if (email == null) return false;

        // Uncomment the line below if you want to restrict to @ddu.ac.in emails only
        // return email.toLowerCase().endsWith("@ddu.ac.in");

        // For now, allow any Google account
        return true;
    }

    /**
     * Create UserProfile from GoogleSignInAccount
     */
    private UserProfile createUserProfile(GoogleSignInAccount account) {
        return new UserProfile(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null
        );
    }

    /**
     * Get user-friendly error message for Google Sign-In errors
     */
    private String getGoogleSignInErrorMessage(int statusCode) {
        switch (statusCode) {
            case 12501: // User cancelled
                return "Sign-in was cancelled";
            case 12502: // Sign-in currently in progress
                return "Sign-in already in progress";
            case 12500: // Sign-in failed
                return "Sign-in failed. Please try again";
            default:
                return "Sign-in failed with error code: " + statusCode;
        }
    }

    // Data classes

    /**
     * User profile class for application use
     */
    public static class UserProfile {
        private String userId;
        private String name;
        private String email;
        private String photoUrl;
        private String role;

        public UserProfile(String userId, String name, String email, String photoUrl) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.photoUrl = photoUrl;
        }

        // Getters and setters
        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhotoUrl() { return photoUrl; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    /**
     * User document class for Firestore storage
     */
    public static class UserDocument {
        private String name;
        private String email;
        private String photoUrl;
        private String role;
        private long createdAt;

        // Default constructor for Firestore
        public UserDocument() {}

        public UserDocument(String name, String email, String photoUrl, String role, long createdAt) {
            this.name = name;
            this.email = email;
            this.photoUrl = photoUrl;
            this.role = role;
            this.createdAt = createdAt;
        }

        // Getters and setters for Firestore
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }
}
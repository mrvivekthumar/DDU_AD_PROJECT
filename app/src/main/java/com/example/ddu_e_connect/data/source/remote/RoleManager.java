package com.example.ddu_e_connect.data.source.remote;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Role Management System for DDU E-Connect
 * Handles user role assignment and checking
 */
public class RoleManager {
    private static final String TAG = "RoleManager";
    private static final String USERS_COLLECTION = "users";

    // Pre-defined admin emails (YOU CAN ADD YOUR EMAIL HERE)
    private static final String[] ADMIN_EMAILS = {
            "mrvivekthumar@gmail.com",           // Admin 1 (from your ContactUsActivity)
            "kuldipvaghasiya0@gmail.com",        // Admin 2 (from your ContactUsActivity)
            "thumarvivekkt@gmail.com",              // ADD YOUR EMAIL HERE
            "vivekthumar334@gmail.com"                    // University admin email
    };

    // Pre-defined helper emails (TEACHERS/HELPERS)
    private static final String[] HELPER_EMAILS = {
            "teacher1@ddu.ac.in",
            "helper1@gmail.com",
            "faculty@ddu.ac.in"
            // ADD MORE TEACHER/HELPER EMAILS HERE
    };

    private FirebaseFirestore db;
    private Context context;

    public interface RoleCallback {
        void onRoleAssigned(String role);
        void onError(String errorMessage);
    }

    public RoleManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Automatically assign role based on email when user first signs in
     */
    public void assignRoleBasedOnEmail(String userEmail, String userId, RoleCallback callback) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            callback.onError("Email cannot be empty");
            return;
        }

        String email = userEmail.toLowerCase().trim();
        String assignedRole = determineRoleFromEmail(email);

        Log.d(TAG, "Assigning role '" + assignedRole + "' to user: " + email);

        // Update user role in Firestore
        updateUserRole(userId, assignedRole, callback);
    }

    /**
     * Determine role based on email address
     */
    private String determineRoleFromEmail(String email) {
        // Check if email is in admin list
        for (String adminEmail : ADMIN_EMAILS) {
            if (email.equals(adminEmail.toLowerCase())) {
                Log.d(TAG, "User identified as ADMIN: " + email);
                return "admin";
            }
        }

        // Check if email is in helper list
        for (String helperEmail : HELPER_EMAILS) {
            if (email.equals(helperEmail.toLowerCase())) {
                Log.d(TAG, "User identified as HELPER: " + email);
                return "helper";
            }
        }

        // Check if email is institutional (@ddu.ac.in)
        if (email.endsWith("@ddu.ac.in")) {
            Log.d(TAG, "Institutional email detected, assigning HELPER role: " + email);
            return "helper";
        }

        // Default role for everyone else
        Log.d(TAG, "User assigned default STUDENT role: " + email);
        return "student";
    }

    /**
     * Update user role in Firestore
     */
    private void updateUserRole(String userId, String role, RoleCallback callback) {
        DocumentReference userDoc = db.collection(USERS_COLLECTION).document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", role);
        updates.put("roleUpdatedAt", System.currentTimeMillis());

        userDoc.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Role updated successfully: " + role);
                    callback.onRoleAssigned(role);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user role", e);
                    callback.onError("Failed to update role: " + e.getMessage());
                });
    }

    /**
     * Manually assign role to user (for admin use)
     */
    public void assignRole(String userId, String newRole, RoleCallback callback) {
        if (!isValidRole(newRole)) {
            callback.onError("Invalid role: " + newRole);
            return;
        }

        Log.d(TAG, "Manually assigning role '" + newRole + "' to user: " + userId);
        updateUserRole(userId, newRole, callback);
    }

    /**
     * Check if role is valid
     */
    private boolean isValidRole(String role) {
        return role != null && (
                role.equals("admin") ||
                        role.equals("helper") ||
                        role.equals("student")
        );
    }

    /**
     * Get user role with caching
     */
    public void getUserRole(String userId, RoleCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("User ID cannot be empty");
            return;
        }

        DocumentReference userDoc = db.collection(USERS_COLLECTION).document(userId);

        userDoc.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null && !role.trim().isEmpty()) {
                            Log.d(TAG, "Retrieved user role: " + role);
                            callback.onRoleAssigned(role);
                        } else {
                            Log.w(TAG, "Role not found, assigning default");
                            callback.onRoleAssigned("student");
                        }
                    } else {
                        Log.w(TAG, "User document not found");
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user role", e);
                    callback.onError("Failed to get role: " + e.getMessage());
                });
    }

    /**
     * Check if current user is admin
     */
    public static boolean isAdmin(String role) {
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * Check if current user is helper
     */
    public static boolean isHelper(String role) {
        return "helper".equalsIgnoreCase(role);
    }

    /**
     * Check if current user can upload
     */
    public static boolean canUpload(String role) {
        return isAdmin(role) || isHelper(role);
    }

    /**
     * Get role display name
     */
    public static String getRoleDisplayName(String role) {
        if (role == null) return "Student";

        switch (role.toLowerCase()) {
            case "admin":
                return "Administrator";
            case "helper":
                return "Helper";
            case "student":
                return "Student";
            default:
                return "Student";
        }
    }

    /**
     * Role Assignment Instructions for Setup
     */
    public static String getRoleSetupInstructions() {
        return "ROLE ASSIGNMENT SETUP:\n\n" +
                "1. ADMIN EMAILS: Add your email to ADMIN_EMAILS array\n" +
                "2. HELPER EMAILS: Add teacher emails to HELPER_EMAILS array\n" +
                "3. INSTITUTIONAL: @ddu.ac.in emails get HELPER role automatically\n" +
                "4. DEFAULT: All other emails get STUDENT role\n\n" +
                "To make yourself admin:\n" +
                "1. Add your email to ADMIN_EMAILS in RoleManager.java\n" +
                "2. Rebuild app\n" +
                "3. Sign out and sign in again\n\n" +
                "Current admin emails:\n" +
                "- mrvivekthumar@gmail.com\n" +
                "- kuldipvaghasiya0@gmail.com";
    }
}
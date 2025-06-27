package com.example.ddu_e_connect.domain.model;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

/**
 * User model class for the application
 * Represents user data from Google Sign-In and Firestore
 */
public class User {
    private String id;
    private String name;
    private String email;
    private String photoUrl;
    private String role;
    private long createdAt;
    private long lastLoginAt;
    private boolean isActive;

    // Default constructor required for Firestore
    public User() {
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
    }

    // Constructor with basic info
    public User(String id, String name, String email) {
        this();
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Constructor with full info
    public User(String id, String name, String email, String photoUrl, String role) {
        this(id, name, email);
        this.photoUrl = photoUrl;
        this.role = role;
    }

    // Constructor from GoogleSignInAccount
    public User(GoogleSignInAccount account) {
        this();
        if (account != null) {
            this.id = account.getId();
            this.name = account.getDisplayName();
            this.email = account.getEmail();
            this.photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;
            this.role = "student"; // Default role
        }
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Utility methods

    /**
     * Check if user has admin privileges
     */
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * Check if user has helper privileges
     */
    public boolean isHelper() {
        return "helper".equalsIgnoreCase(role);
    }

    /**
     * Check if user is a student
     */
    public boolean isStudent() {
        return "student".equalsIgnoreCase(role);
    }

    /**
     * Check if user can upload PDFs
     */
    public boolean canUploadPdf() {
        return isAdmin() || isHelper();
    }

    /**
     * Check if user has institutional email
     */
    public boolean hasInstitutionalEmail() {
        return email != null && email.toLowerCase().endsWith("@ddu.ac.in");
    }

    /**
     * Get user's first name
     */
    public String getFirstName() {
        if (name != null && !name.trim().isEmpty()) {
            String[] nameParts = name.trim().split("\\s+");
            return nameParts[0];
        }
        return "User";
    }

    /**
     * Get user's initials for profile picture placeholder
     */
    public String getInitials() {
        if (name != null && !name.trim().isEmpty()) {
            String[] nameParts = name.trim().split("\\s+");
            StringBuilder initials = new StringBuilder();

            for (int i = 0; i < Math.min(2, nameParts.length); i++) {
                if (!nameParts[i].isEmpty()) {
                    initials.append(nameParts[i].charAt(0));
                }
            }

            return initials.toString().toUpperCase();
        }
        return "U";
    }

    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lastLoginAt = System.currentTimeMillis();
    }

    /**
     * Check if this is a new user (created today)
     */
    public boolean isNewUser() {
        long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        return createdAt > oneDayAgo;
    }

    /**
     * Get role display name
     */
    public String getRoleDisplayName() {
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

    // Override methods

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        User user = (User) obj;
        return id != null ? id.equals(user.id) : user.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Create a copy of this user
     */
    public User copy() {
        User copy = new User(this.id, this.name, this.email, this.photoUrl, this.role);
        copy.setCreatedAt(this.createdAt);
        copy.setLastLoginAt(this.lastLoginAt);
        copy.setActive(this.isActive);
        return copy;
    }

    /**
     * Convert to simple map for logging (without sensitive data)
     */
    public String toLogString() {
        return "User{" +
                "id='" + (id != null ? id.substring(0, Math.min(8, id.length())) + "..." : "null") + '\'' +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
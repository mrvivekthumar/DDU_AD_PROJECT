package com.example.ddu_e_connect.presentation.view.contact;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;

public class ContactUsActivity extends AppCompatActivity {
    private static final String TAG = "ContactUsActivity";

    // Admin contact information
    private static final String ADMIN1_EMAIL = "mrvivekthumar@gmail.com";
    private static final String ADMIN2_EMAIL = "kuldipvaghasiya0@gmail.com";
    private static final String ADMIN1_NAME = "Vivek Thumar";
    private static final String ADMIN2_NAME = "Kuldip Vaghasiya";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        setupUI();
        setupClickListeners();
        applyAnimations();

        Log.d(TAG, "ContactUsActivity initialized with enhanced UI");
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        // Admin 1 elements
        TextView admin1Name = findViewById(R.id.admin1_name);
        TextView admin1Email = findViewById(R.id.admin1_email);
        ImageView admin1Image = findViewById(R.id.admin1_image);

        // Admin 2 elements
        TextView admin2Name = findViewById(R.id.admin2_name);
        TextView admin2Email = findViewById(R.id.admin2_email);
        ImageView admin2Image = findViewById(R.id.admin2_image);

        // Set admin information
        if (admin1Name != null) admin1Name.setText(ADMIN1_NAME);
        if (admin1Email != null) admin1Email.setText(ADMIN1_EMAIL);
        if (admin1Image != null) admin1Image.setImageResource(R.drawable.admin1_image);

        if (admin2Name != null) admin2Name.setText(ADMIN2_NAME);
        if (admin2Email != null) admin2Email.setText(ADMIN2_EMAIL);
        if (admin2Image != null) admin2Image.setImageResource(R.drawable.admin2_image);

        Log.d(TAG, "UI components setup completed");
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        // Admin 1 card click listeners
        setupAdminCardClickListeners(R.id.admin_card_1, ADMIN1_NAME, ADMIN1_EMAIL);

        // Admin 2 card click listeners
        setupAdminCardClickListeners(R.id.admin_card_2, ADMIN2_NAME, ADMIN2_EMAIL);

        // Profile image click listeners for fun interaction
        setupProfileImageClickListeners();

        Log.d(TAG, "Click listeners setup completed");
    }

    /**
     * Setup click listeners for admin cards
     */
    private void setupAdminCardClickListeners(int cardId, String adminName, String adminEmail) {
        View adminCard = findViewById(cardId);
        if (adminCard != null) {
            adminCard.setOnClickListener(v -> {
                addCardClickAnimation(v);
                showContactOptions(adminName, adminEmail);
            });
        }
    }

    /**
     * Setup profile image click listeners
     */
    private void setupProfileImageClickListeners() {
        ImageView admin1Image = findViewById(R.id.admin1_image);
        ImageView admin2Image = findViewById(R.id.admin2_image);

        if (admin1Image != null) {
            admin1Image.setOnClickListener(v -> {
                addProfileClickAnimation(v);
                showInfo("👋 Hello from " + ADMIN1_NAME + "!");
            });
        }

        if (admin2Image != null) {
            admin2Image.setOnClickListener(v -> {
                addProfileClickAnimation(v);
                showInfo("👋 Hello from " + ADMIN2_NAME + "!");
            });
        }
    }

    /**
     * Add click animation to cards
     */
    private void addCardClickAnimation(View view) {
        view.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    /**
     * Add click animation to profile images
     */
    private void addProfileClickAnimation(View view) {
        view.animate()
                .rotationY(360f)
                .setDuration(800)
                .withEndAction(() -> view.setRotationY(0f))
                .start();
    }

    /**
     * Apply entrance animations
     */
    private void applyAnimations() {
        View adminCard1 = findViewById(R.id.admin_card_1);
        View adminCard2 = findViewById(R.id.admin_card_2);

        if (adminCard1 != null && adminCard2 != null) {
            // Load animations
            Animation slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
            Animation slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);

            // Apply animations with delay
            adminCard1.startAnimation(slideInLeft);

            adminCard2.postDelayed(() -> {
                adminCard2.startAnimation(slideInRight);
            }, 200);

            Log.d(TAG, "Entrance animations applied");
        }
    }

    /**
     * Show contact options for selected admin
     */
    private void showContactOptions(String adminName, String adminEmail) {
        String[] options = {
                "Send Email",
                "Copy Email Address",
                "View Profile Info"
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Contact " + adminName)
                .setIcon(R.drawable.person_icon)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            sendEmail(adminName, adminEmail);
                            break;
                        case 1:
                            copyEmailToClipboard(adminEmail);
                            break;
                        case 2:
                            showProfileInfo(adminName, adminEmail);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();

        Log.d(TAG, "Contact options shown for: " + adminName);
    }

    /**
     * Send email to admin
     */
    private void sendEmail(String adminName, String adminEmail) {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + adminEmail));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DDU E-Connect - Message from App User");
            emailIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hello " + adminName + ",\n\n" +
                            "I'm reaching out regarding the DDU E-Connect app.\n\n" +
                            "Message:\n\n\n" +
                            "Best regards,\n" +
                            "[Your Name]");

            startActivity(Intent.createChooser(emailIntent, "Send email to " + adminName));
            showSuccess("Opening email app...");
            Log.d(TAG, "Email intent sent to: " + adminEmail);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send email", e);
            showError("No email app found on your device");
        }
    }

    /**
     * Copy email to clipboard
     */
    private void copyEmailToClipboard(String email) {
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Email", email);
            clipboard.setPrimaryClip(clip);

            showSuccess("Email address copied to clipboard! 📋");
            Log.d(TAG, "Email copied to clipboard: " + email);
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy email to clipboard", e);
            showError("Failed to copy email address");
        }
    }

    /**
     * Show profile information
     */
    private void showProfileInfo(String adminName, String adminEmail) {
        String profileInfo;

        if (adminName.equals(ADMIN1_NAME)) {
            profileInfo = "👨‍💻 " + ADMIN1_NAME + "\n\n" +
                    "Role: Lead Developer\n" +
                    "Specialization: Android Development, UI/UX Design\n" +
                    "Experience: Mobile App Development\n\n" +
                    "📧 Email: " + ADMIN1_EMAIL + "\n\n" +
                    "Vivek is the lead developer behind DDU E-Connect, " +
                    "passionate about creating user-friendly mobile applications " +
                    "for the university community.";
        } else {
            profileInfo = "👨‍💻 " + ADMIN2_NAME + "\n\n" +
                    "Role: Co-Developer\n" +
                    "Specialization: Backend Development, Database Management\n" +
                    "Experience: Software Development\n\n" +
                    "📧 Email: " + ADMIN2_EMAIL + "\n\n" +
                    "Kuldip is the co-developer of DDU E-Connect, " +
                    "focusing on backend systems and ensuring smooth " +
                    "app performance for all users.";
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Profile Information")
                .setMessage(profileInfo)
                .setPositiveButton("Send Email", (dialog, which) -> sendEmail(adminName, adminEmail))
                .setNegativeButton("Close", null)
                .show();

        Log.d(TAG, "Profile info shown for: " + adminName);
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Show info message
     */
    private void showInfo(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        // Add smooth back navigation
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ContactUsActivity destroyed");
    }
}
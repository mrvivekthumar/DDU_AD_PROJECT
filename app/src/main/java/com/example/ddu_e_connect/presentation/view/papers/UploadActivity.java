package com.example.ddu_e_connect.presentation.view.papers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.data.source.remote.GoogleAuthRepository;
import com.example.ddu_e_connect.data.source.remote.FirebaseStorageRepository; // ✅ NEW: Firebase Storage
import com.example.ddu_e_connect.data.source.remote.RoleManager;
import com.example.ddu_e_connect.databinding.ActivityUploadBinding;
import com.example.ddu_e_connect.presentation.view.auth.SignInActivity;
import com.example.ddu_e_connect.presentation.view.home.HomeActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "UploadActivity";

    // UI Components
    private ActivityUploadBinding binding;
    private Spinner categorySpinner;
    private Spinner folderSpinner;
    private EditText pdfNameEditText;
    private Button selectPdfButton;
    private Button uploadPdfButton;
    private ProgressBar uploadProgressBar;
    private TextView uploadStatusText;
    private ImageView fileSelectedIcon;
    private ImageView fileNotSelectedIcon;
    private View progressSection;


    // ✅ UPDATED: Services - Replaced Google Drive with Firebase Storage
    private GoogleAuthRepository authRepository;
    private FirebaseStorageRepository storageRepository; // ✅ NEW: Firebase Storage Repository
    private GoogleSignInAccount currentUser;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    // Upload Data
    private Uri selectedPdfUri;
    private String selectedCategory = "academic";
    private String selectedFolderName = "";
    private List<FirebaseStorageRepository.StorageFolder> availableFolders = new ArrayList<>(); // ✅ UPDATED: Storage folders

    // Categories for PDF organization
    private final String[] categories = {
            "Academic Papers", "Study Materials", "Exam Papers", "Club Documents"
    };
    private final String[] categoryKeys = {
            "academic", "study", "exam", "club"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupUI();
        checkUserPermissions();

        Log.d(TAG, "✅ UploadActivity initialized with Firebase Storage");
    }

    /**
     * ✅ UPDATED: Initialize components with Firebase Storage
     */
    private void initializeComponents() {
        // Initialize repositories
        authRepository = new GoogleAuthRepository(this);
        storageRepository = new FirebaseStorageRepository(this); // ✅ NEW: Firebase Storage instead of Google Drive
        currentUser = authRepository.getCurrentUser();

        // Initialize UI components
        initializeUIComponents();

        // Setup PDF picker launcher
        setupPdfPickerLauncher();
    }

    /**
     * Initialize UI components
     */
    private void initializeUIComponents() {
        // Find UI components
        pdfNameEditText = findViewById(R.id.pdfNameEditText);
        selectPdfButton = findViewById(R.id.selectPdfButton);
        uploadPdfButton = findViewById(R.id.uploadPdfButton);

        // Progress components
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        uploadStatusText = findViewById(R.id.uploadStatusText);
        progressSection = findViewById(R.id.progressSection);

        // File selection indicators
        fileSelectedIcon = findViewById(R.id.fileSelectedIcon);
        fileNotSelectedIcon = findViewById(R.id.fileNotSelectedIcon);

        // Spinners
        categorySpinner = findViewById(R.id.categorySpinner);
        folderSpinner = findViewById(R.id.folderSpinner);

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Quick action buttons
        findViewById(R.id.viewPapersButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, PapersActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.helpButton).setOnClickListener(v -> {
            Intent intent = new Intent(this,
                    com.example.ddu_e_connect.presentation.view.contact.ContactUsActivity.class);
            startActivity(intent);
        });

        // Initialize spinners
        setupSpinners();
    }

    /**
     * ✅ UPDATED: Setup spinners with Firebase Storage folder loading
     */
    private void setupSpinners() {
        // Setup category spinner
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = categoryKeys[position];
                Log.d(TAG, "Category selected: " + selectedCategory);
                loadFoldersForCategory(); // ✅ UPDATED: Load folders from Firebase Storage
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup folder spinner
        updateFolderSpinner();

        folderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // "Select Folder" option
                    selectedFolderName = "";
                } else if (position == availableFolders.size() + 1) {
                    // "Create New Folder" option
                    showCreateFolderDialog();
                } else {
                    // Existing folder selected
                    FirebaseStorageRepository.StorageFolder selectedFolder = availableFolders.get(position - 1);
                    selectedFolderName = selectedFolder.getName();
                    Log.d(TAG, "Folder selected: " + selectedFolderName);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Setup PDF picker launcher
     */
    private void setupPdfPickerLauncher() {
        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handlePdfPickerResult(result);
                    }
                }
        );
    }

    /**
     * Setup UI event listeners
     */
    private void setupUI() {
        // Select PDF button
        selectPdfButton.setOnClickListener(v -> selectPdfFile());

        // Upload PDF button
        uploadPdfButton.setOnClickListener(v -> validateAndUploadPdf());

        // Initially disable upload button
        updateUploadButtonState();

        // ✅ UPDATED: Load initial folders from Firebase Storage
        loadFoldersForCategory();

        setupTextInputListeners();
    }

    /**
     * Add text change listener to PDF name field
     */
    private void setupTextInputListeners() {
        pdfNameEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateUploadButtonState();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    /**
     * ✅ ENHANCED: Check user permissions with detailed logging
     */
    private void checkUserPermissions() {
        if (currentUser == null) {
            Log.w(TAG, "No user signed in");
            showUnauthorizedDialog("unknown");
            navigateToSignIn();
            return;
        }

        Log.d(TAG, "Checking upload permissions for user: " + currentUser.getEmail());

        authRepository.fetchUserRole(currentUser.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                Log.d(TAG, "User role fetched: " + role);

                boolean canUpload = RoleManager.canUpload(role);
                Log.d(TAG, "Can upload: " + canUpload + " (Role: " + role + ")");

                if (canUpload) {
                    enableUploadFeatures(role);
                } else {
                    showUnauthorizedDialog(role);
                    disableUploadFeatures();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to fetch user role: " + errorMessage);
                attemptRoleReassignment();
            }
        });
    }

    /**
     * Attempt role reassignment if role fetch fails
     */
    private void attemptRoleReassignment() {
        if (currentUser == null) return;

        Log.d(TAG, "Attempting role reassignment for: " + currentUser.getEmail());

        RoleManager roleManager = new RoleManager(this);
        roleManager.assignRoleBasedOnEmail(
                currentUser.getEmail(),
                currentUser.getId(),
                new RoleManager.RoleCallback() {
                    @Override
                    public void onRoleAssigned(String role) {
                        Log.d(TAG, "Role reassigned successfully: " + role);

                        boolean canUpload = RoleManager.canUpload(role);
                        if (canUpload) {
                            enableUploadFeatures(role);
                        } else {
                            showUnauthorizedDialog(role);
                            disableUploadFeatures();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Role reassignment failed: " + errorMessage);
                        showUnauthorizedDialog("unknown");
                        disableUploadFeatures();
                    }
                }
        );
    }

    /**
     * Show welcome message for authorized users
     */
    private void showWelcomeMessage(String role) {
        String message;

        if ("admin".equalsIgnoreCase(role)) {
            message = "👑 Welcome Admin! You have full upload access.";
        } else if ("helper".equalsIgnoreCase(role)) {
            message = "🤝 Welcome Helper! Ready to upload educational content.";
        } else {
            message = "✅ Upload access granted!";
        }

        showSuccess(message);
    }

    /**
     * Show detailed unauthorized dialog
     */
    private void showUnauthorizedDialog(String userRole) {
        String title = "🔒 Upload Permission Required";
        String message = buildUnauthorizedMessage(userRole);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_lock)
                .setPositiveButton("📧 Contact Admin", (dialog, which) -> {
                    contactAdminForPermission();
                })
                .setNeutralButton("ℹ️ View Papers", (dialog, which) -> {
                    Intent intent = new Intent(this, PapersActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("🏠 Go Home", (dialog, which) -> {
                    navigateToHome();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Contact admin for permission with pre-filled email
     */
    private void contactAdminForPermission() {
        try {
            String adminEmail = "mrvivekthumar@gmail.com";
            String subject = "DDU E-Connect: Upload Permission Request";
            String emailBody = buildPermissionRequestEmail();

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + adminEmail));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);

            startActivity(Intent.createChooser(emailIntent, "Send Permission Request"));
            Log.d(TAG, "Permission request email intent created");

        } catch (Exception e) {
            Log.e(TAG, "Failed to open email for permission request", e);
            Intent contactIntent = new Intent(this,
                    com.example.ddu_e_connect.presentation.view.contact.ContactUsActivity.class);
            startActivity(contactIntent);
        }
    }

    /**
     * Build detailed unauthorized message
     */
    private String buildUnauthorizedMessage(String userRole) {
        StringBuilder message = new StringBuilder();

        message.append("🚫 Upload access denied.\n\n");
        message.append("👤 Your role: ").append(userRole.toUpperCase()).append("\n");
        message.append("✅ Required: ADMIN or HELPER\n\n");

        if ("student".equalsIgnoreCase(userRole)) {
            message.append("📚 As a STUDENT, you can:\n");
            message.append("• ✅ View all study materials\n");
            message.append("• ✅ Download exam papers\n");
            message.append("• ✅ Access club documents\n");
            message.append("• ❌ Upload files\n\n");

            message.append("🎯 To get upload access:\n");
            message.append("• Contact administrator\n");
            message.append("• Request HELPER role (for faculty)\n");
            message.append("• Use institutional email (@ddu.ac.in)\n");
        } else {
            message.append("❓ Role verification failed.\n");
            message.append("Please contact support.\n");
        }

        return message.toString();
    }

    /**
     * Build permission request email body
     */
    private String buildPermissionRequestEmail() {
        StringBuilder body = new StringBuilder();

        body.append("Dear Admin,\n\n");
        body.append("I am requesting upload permission for DDU E-Connect app.\n\n");

        if (currentUser != null) {
            body.append("📋 My Details:\n");
            body.append("• Name: ").append(currentUser.getDisplayName()).append("\n");
            body.append("• Email: ").append(currentUser.getEmail()).append("\n");
            body.append("• Current Role: Student\n");
            body.append("• Requested Role: Helper/Admin\n\n");
        }

        body.append("📝 Reason for Request:\n");
        body.append("[ Please describe why you need upload permission ]\n\n");

        body.append("🎯 I would like to:\n");
        body.append("□ Upload study materials for students\n");
        body.append("□ Share exam papers\n");
        body.append("□ Upload club documents\n");
        body.append("□ Help organize academic resources\n\n");

        body.append("Thank you for considering my request.\n\n");
        body.append("Best regards,\n");
        if (currentUser != null) {
            body.append(currentUser.getDisplayName());
        }

        return body.toString();
    }

    /**
     * ✅ UPDATED: Enable upload features (no more Google Drive permission request)
     */
    private void enableUploadFeatures(String role) {
        selectPdfButton.setEnabled(true);
        pdfNameEditText.setEnabled(true);
        categorySpinner.setEnabled(true);
        folderSpinner.setEnabled(true);

        // Show welcome message
        showSuccess("Welcome " + role.toUpperCase() + "! Firebase Storage ready for uploads.");

        // ✅ REMOVED: No more Google Drive permission request needed!
        // Firebase Storage works automatically with Firebase Authentication

        Log.d(TAG, "✅ Upload features enabled for " + role + " - Firebase Storage ready!");
    }

    /**
     * Disable upload features for unauthorized users
     */
    private void disableUploadFeatures() {
        selectPdfButton.setEnabled(false);
        uploadPdfButton.setEnabled(false);
        pdfNameEditText.setEnabled(false);

        selectPdfButton.setText("Upload Not Allowed");
        uploadPdfButton.setText("Permission Required");
    }

    /**
     * ✅ UPDATED: Load folders using Firebase Storage
     */
    private void loadFoldersForCategory() {
        Log.d(TAG, "Loading folders for category: " + selectedCategory);

        storageRepository.getFoldersInCategory(selectedCategory, new FirebaseStorageRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<FirebaseStorageRepository.StorageFolder> folders) {
                Log.d(TAG, "✅ Loaded " + folders.size() + " folders from Firebase Storage");
                availableFolders.clear();
                availableFolders.addAll(folders);
                updateFolderSpinner();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load folders from Firebase Storage: " + errorMessage);
                showError("Failed to load folders: " + errorMessage);
            }
        });
    }

    // Add these enhanced methods to your UploadActivity.java for beautiful animations:

    /**
     * ✅ ENHANCED: Show create folder dialog with smooth animations
     */
    private void showCreateFolderDialog() {
        // Create custom dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_folder, null);
        dialog.setContentView(dialogView);

        // Make dialog width match parent with margins
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            // Add smooth slide-up animation
            window.getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }

        // Find views
        EditText folderNameInput = dialogView.findViewById(R.id.folderNameInput);
        TextView categoryText = dialogView.findViewById(R.id.categoryText);
        Button createButton = dialogView.findViewById(R.id.createButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set category text with animation
        String categoryTitle = categories[getSelectedCategoryIndex()];
        categoryText.setText("in " + categoryTitle);

        // ✅ BEAUTIFUL: Add entrance animations
        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.8f);
        dialogView.setScaleY(0.8f);
        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                .start();

        // ✅ BEAUTIFUL: Add button hover effects
        setupButtonAnimation(createButton);
        setupButtonAnimation(cancelButton);

        // Set up click listeners with animations
        createButton.setOnClickListener(v -> {
            // Add click animation
            animateButtonClick(v, () -> {
                String folderName = folderNameInput.getText().toString().trim();
                if (!folderName.isEmpty()) {
                    if (isValidFileName(folderName)) {
                        selectedFolderName = folderName;
                        availableFolders.add(new FirebaseStorageRepository.StorageFolder(folderName, "new"));
                        updateFolderSpinner();

                        int newPosition = availableFolders.size();
                        folderSpinner.setSelection(newPosition);

                        // ✅ BEAUTIFUL: Smooth dialog dismiss with animation
                        dismissDialogWithAnimation(dialog, dialogView);

                        // Show success with bounce animation
                        showAnimatedSuccess("📁 Folder '" + folderName + "' ready to use!");
                    } else {
                        // ✅ BEAUTIFUL: Shake animation for error
                        shakeView(folderNameInput);
                        folderNameInput.setError("Use only letters, numbers, spaces, hyphens, and underscores");
                        folderNameInput.requestFocus();
                    }
                } else {
                    shakeView(folderNameInput);
                    folderNameInput.setError("Please enter a folder name");
                    folderNameInput.requestFocus();
                }
            });
        });

        cancelButton.setOnClickListener(v -> {
            animateButtonClick(v, () -> {
                dismissDialogWithAnimation(dialog, dialogView);
                folderSpinner.setSelection(0);
            });
        });

        // ✅ BEAUTIFUL: Smooth keyboard appearance
        dialog.setOnShowListener(dialogInterface -> {
            folderNameInput.requestFocus();
            android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(() -> {
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(folderNameInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }, 200);
        });

        dialog.show();
        Log.d(TAG, "✅ Beautiful animated create folder dialog shown");
    }

    /**
     * ✅ ENHANCED: Success dialog with celebration animation
     */
    private void onUploadSuccess(String fileName, String downloadUrl) {
        setUploadingState(false);

        Log.d(TAG, "✅ Upload completed successfully: " + fileName);

        // Create custom success dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_success, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Find views and set data
        TextView fileNameText = dialogView.findViewById(R.id.fileName);
        TextView categoryInfo = dialogView.findViewById(R.id.categoryInfo);
        Button uploadAnotherButton = dialogView.findViewById(R.id.uploadAnotherButton);
        Button goHomeButton = dialogView.findViewById(R.id.goHomeButton);
        androidx.cardview.widget.CardView successIcon = dialogView.findViewById(R.id.successIcon); // Assuming you add an ID

        fileNameText.setText(fileName);
        categoryInfo.setText(categories[getSelectedCategoryIndex()]);

        // ✅ BEAUTIFUL: Add celebration entrance animation
        dialogView.setAlpha(0f);
        dialogView.setTranslationY(100f);
        dialogView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // ✅ BEAUTIFUL: Success icon bounce animation
        new android.os.Handler().postDelayed(() -> {
            if (successIcon != null) {
                successIcon.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            successIcon.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(200)
                                    .start();
                        })
                        .start();
            }

            // ✅ BEAUTIFUL: Text animation
            fileNameText.setAlpha(0f);
            fileNameText.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(200)
                    .start();
        }, 300);

        // Add button animations
        setupButtonAnimation(uploadAnotherButton);
        setupButtonAnimation(goHomeButton);

        // Set up click listeners
        uploadAnotherButton.setOnClickListener(v -> {
            animateButtonClick(v, () -> {
                dismissDialogWithAnimation(dialog, dialogView);
                resetUploadForm();
                showAnimatedSuccess("Ready for another upload! 📤");
            });
        });

        goHomeButton.setOnClickListener(v -> {
            animateButtonClick(v, () -> {
                dismissDialogWithAnimation(dialog, dialogView);
                navigateToHome();
            });
        });

        dialog.show();

        // ✅ BEAUTIFUL: Success haptic feedback pattern
        performSuccessHaptic();

        Log.d(TAG, "✅ Beautiful animated success dialog shown");
    }

    /**
     * ✅ BEAUTIFUL: Setup button hover/press animations
     */
    private void setupButtonAnimation(Button button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // Press animation
                    v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator())
                            .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // Release animation
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                            .start();
                    break;
            }
            return false; // Let the click event continue
        });
    }

    /**
     * ✅ BEAUTIFUL: Animate button click with callback
     */
    private void animateButtonClick(View button, Runnable callback) {
        button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .withEndAction(callback)
                            .start();
                })
                .start();
    }

    /**
     * ✅ BEAUTIFUL: Smooth dialog dismiss animation
     */
    private void dismissDialogWithAnimation(Dialog dialog, View dialogView) {
        dialogView.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(250)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                })
                .start();
    }

    /**
     * ✅ BEAUTIFUL: Shake animation for errors
     */
    private void shakeView(View view) {
        android.view.animation.AnimationSet animSet = new android.view.animation.AnimationSet(true);
        android.view.animation.TranslateAnimation shake = new android.view.animation.TranslateAnimation(
                android.view.animation.Animation.RELATIVE_TO_SELF, 0f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.05f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0f);
        shake.setDuration(50);
        shake.setRepeatCount(5);
        shake.setRepeatMode(android.view.animation.Animation.REVERSE);

        animSet.addAnimation(shake);
        view.startAnimation(animSet);

        // Add haptic feedback for error
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
    }

    /**
     * ✅ BEAUTIFUL: Animated success message
     */
    private void showAnimatedSuccess(String message) {
        // Create floating success message
        Dialog successToast = new Dialog(this);
        successToast.requestWindowFeature(Window.FEATURE_NO_TITLE);
        successToast.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Create layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setBackgroundResource(R.drawable.custom_button);
        layout.setPadding(32, 20, 32, 20);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Success icon with glow effect
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_upload);
        icon.setColorFilter(getColor(R.color.button_text_color));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(56, 56);
        iconParams.setMargins(0, 0, 20, 0);
        icon.setLayoutParams(iconParams);
        layout.addView(icon);

        // Message text
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(getColor(R.color.button_text_color));
        text.setTextSize(18);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(text);

        successToast.setContentView(layout);

        // Position at top center
        Window window = successToast.getWindow();
        if (window != null) {
            window.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
            window.getAttributes().y = 150;
        }

        // ✅ BEAUTIFUL: Slide down entrance animation
        layout.setTranslationY(-200f);
        layout.setAlpha(0f);

        successToast.show();

        layout.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

        // Icon pulse animation
        icon.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(300)
                .withEndAction(() -> {
                    icon.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start();
                })
                .start();

        // ✅ BEAUTIFUL: Slide up exit animation
        new android.os.Handler().postDelayed(() -> {
            layout.animate()
                    .translationY(-200f)
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        if (successToast.isShowing()) {
                            successToast.dismiss();
                        }
                    })
                    .start();
        }, 2500);
    }

    /**
     * ✅ BEAUTIFUL: Success haptic feedback pattern
     */
    private void performSuccessHaptic() {
        // Double tap haptic pattern for success
        android.os.Handler handler = new android.os.Handler();

        // First tap
        uploadPdfButton.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);

        // Second tap after 100ms
        handler.postDelayed(() -> {
            uploadPdfButton.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }, 100);
    }

    /**
     * ✅ BEAUTIFUL: Enhanced unauthorized dialog with animations
     */
    private void showUnauthorizedDialog(String userRole) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unauthorized, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Find views and set data
        TextView userRoleText = dialogView.findViewById(R.id.userRoleText);
        Button contactAdminButton = dialogView.findViewById(R.id.contactAdminButton);
        Button viewPapersButton = dialogView.findViewById(R.id.viewPapersButton);
        Button goHomeButton = dialogView.findViewById(R.id.goHomeButton);

        userRoleText.setText(userRole.toUpperCase());

        // ✅ BEAUTIFUL: Error entrance animation
        dialogView.setAlpha(0f);
        dialogView.setTranslationY(50f);
        dialogView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Add button animations
        setupButtonAnimation(contactAdminButton);
        setupButtonAnimation(viewPapersButton);
        setupButtonAnimation(goHomeButton);

        // Set up click listeners with animations
        contactAdminButton.setOnClickListener(v -> {
            animateButtonClick(v, () -> {
                dismissDialogWithAnimation(dialog, dialogView);
                contactAdminForPermission();
            });
        });

        viewPapersButton.setOnClickListener(v -> {
            animateButtonClick(v, () -> {
                dismissDialogWithAnimation(dialog, dialogView);
                Intent intent = new Intent(this, PapersActivity.class);
                startActivity(intent);
                finish();
            });
        });

        goHomeButton.setOnClickListener(v -> {
            animateButtonClick(v, () -> {
                dismissDialogWithAnimation(dialog, dialogView);
                navigateToHome();
            });
        });

        dialog.show();

        // ✅ BEAUTIFUL: Warning haptic feedback
        contactAdminButton.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

        Log.d(TAG, "✅ Beautiful animated unauthorized dialog shown for role: " + userRole);
    }

    // ✅ UPDATE: Enhanced error and success methods
    private void showError(String message) {
        // Create animated error toast
        showAnimatedError(message);
        Log.e(TAG, "Error: " + message);
    }

    private void showSuccess(String message) {
        showAnimatedSuccess(message);
        Log.d(TAG, "Success: " + message);
    }

    /**
     * ✅ BEAUTIFUL: Animated error message
     */
    private void showAnimatedError(String message) {
        Dialog errorToast = new Dialog(this);
        errorToast.requestWindowFeature(Window.FEATURE_NO_TITLE);
        errorToast.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setBackgroundColor(getColor(android.R.color.holo_red_dark));
        layout.setPadding(32, 20, 32, 20);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Error icon
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_lock);
        icon.setColorFilter(getColor(android.R.color.white));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
        iconParams.setMargins(0, 0, 16, 0);
        icon.setLayoutParams(iconParams);
        layout.addView(icon);

        // Error text
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(getColor(android.R.color.white));
        text.setTextSize(16);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(text);

        errorToast.setContentView(layout);

        Window window = errorToast.getWindow();
        if (window != null) {
            window.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
            window.getAttributes().y = 150;
        }

        // Shake entrance animation
        layout.setTranslationX(-30f);
        layout.setAlpha(0f);

        errorToast.show();

        layout.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();

        // Auto dismiss with fade
        new android.os.Handler().postDelayed(() -> {
            layout.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (errorToast.isShowing()) {
                            errorToast.dismiss();
                        }
                    })
                    .start();
        }, 3000);

        // Error haptic feedback
        icon.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
    }

    /**
     * Update folder spinner with available folders
     */
    private void updateFolderSpinner() {
        List<String> folderNames = new ArrayList<>();
        folderNames.add("Select Folder (Optional)");

        for (FirebaseStorageRepository.StorageFolder folder : availableFolders) {
            folderNames.add(folder.getName());
        }

        folderNames.add("+ Create New Folder");

        ArrayAdapter<String> folderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, folderNames);
        folderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderSpinner.setAdapter(folderAdapter);
    }

    /**
     * ✅ BEAUTIFUL: Show custom create folder dialog
     */
    private void showCreateFolderDialog() {
        // Create custom dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_folder, null);
        dialog.setContentView(dialogView);

        // Make dialog width match parent with margins
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Find views
        EditText folderNameInput = dialogView.findViewById(R.id.folderNameInput);
        TextView categoryText = dialogView.findViewById(R.id.categoryText);
        Button createButton = dialogView.findViewById(R.id.createButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set category text
        String categoryTitle = categories[getSelectedCategoryIndex()];
        categoryText.setText("in " + categoryTitle);

        // Set up click listeners
        createButton.setOnClickListener(v -> {
            String folderName = folderNameInput.getText().toString().trim();
            if (!folderName.isEmpty()) {
                if (isValidFileName(folderName)) {
                    // ✅ Firebase Storage creates folders automatically when first file is uploaded
                    selectedFolderName = folderName;

                    // Add to folder list for immediate UI update
                    availableFolders.add(new FirebaseStorageRepository.StorageFolder(folderName, "new"));
                    updateFolderSpinner();

                    // Select the new folder in spinner
                    int newPosition = availableFolders.size(); // Position after "Select Folder"
                    folderSpinner.setSelection(newPosition);

                    dialog.dismiss();
                    showBeautifulSuccess("📁 Folder '" + folderName + "' will be created when you upload a file!");
                } else {
                    folderNameInput.setError("Use only letters, numbers, spaces, hyphens, and underscores");
                    folderNameInput.requestFocus();
                }
            } else {
                folderNameInput.setError("Please enter a folder name");
                folderNameInput.requestFocus();
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            folderSpinner.setSelection(0); // Reset to "Select Folder"
        });

        // Show dialog with animation
        dialog.show();

        // Focus on input and show keyboard
        folderNameInput.requestFocus();

        Log.d(TAG, "✅ Beautiful create folder dialog shown");
    }

    /**
     * ✅ BEAUTIFUL: Show custom upload success dialog
     */
    private void onUploadSuccess(String fileName, String downloadUrl) {
        setUploadingState(false);

        Log.d(TAG, "✅ Upload completed successfully: " + fileName);
        Log.d(TAG, "📁 Download URL: " + downloadUrl);

        // Create custom success dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false); // Prevent dismiss by tapping outside

        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_success, null);
        dialog.setContentView(dialogView);

        // Make dialog width match parent with margins
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Find views and set data
        TextView fileNameText = dialogView.findViewById(R.id.fileName);
        TextView categoryInfo = dialogView.findViewById(R.id.categoryInfo);
        Button uploadAnotherButton = dialogView.findViewById(R.id.uploadAnotherButton);
        Button goHomeButton = dialogView.findViewById(R.id.goHomeButton);

        // Set file information
        fileNameText.setText(fileName);
        categoryInfo.setText(categories[getSelectedCategoryIndex()]);

        // Set up click listeners
        uploadAnotherButton.setOnClickListener(v -> {
            dialog.dismiss();
            resetUploadForm();
            showBeautifulSuccess("Ready for another upload! 📤");
        });

        goHomeButton.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToHome();
        });

        // Show dialog with animation
        dialog.show();

        // Add success haptic feedback
        uploadAnotherButton.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);

        Log.d(TAG, "✅ Beautiful success dialog shown");
    }

    /**
     * ✅ BEAUTIFUL: Show custom unauthorized dialog
     */
    private void showUnauthorizedDialog(String userRole) {
        // Create custom dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false); // Prevent dismiss by tapping outside

        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unauthorized, null);
        dialog.setContentView(dialogView);

        // Make dialog width match parent with margins
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Find views and set data
        TextView userRoleText = dialogView.findViewById(R.id.userRoleText);
        Button contactAdminButton = dialogView.findViewById(R.id.contactAdminButton);
        Button viewPapersButton = dialogView.findViewById(R.id.viewPapersButton);
        Button goHomeButton = dialogView.findViewById(R.id.goHomeButton);

        // Set user role
        userRoleText.setText(userRole.toUpperCase());

        // Set up click listeners
        contactAdminButton.setOnClickListener(v -> {
            dialog.dismiss();
            contactAdminForPermission();
        });

        viewPapersButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, PapersActivity.class);
            startActivity(intent);
            finish();
        });

        goHomeButton.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToHome();
        });

        // Show dialog
        dialog.show();

        Log.d(TAG, "✅ Beautiful unauthorized dialog shown for role: " + userRole);
    }

    /**
     * ✅ BEAUTIFUL: Show custom PDF open options dialog
     */
    private void showPdfOpenOptions(String downloadUrl, String fileName) {
        // Create custom dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Create a simple custom layout for PDF options
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(32, 32, 32, 32);
        dialogLayout.setBackgroundResource(R.drawable.custom_edittext);

        // Title
        TextView title = new TextView(this);
        title.setText("📄 Open: " + fileName);
        title.setTextColor(getColor(R.color.text_color1));
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 24);
        dialogLayout.addView(title);

        // Option buttons
        String[] options = {"🌐 Open in Browser", "📱 Open in PDF App", "📋 Copy Link", "❌ Cancel"};

        for (int i = 0; i < options.length; i++) {
            Button optionButton = new Button(this);
            optionButton.setText(options[i]);
            optionButton.setBackgroundResource(R.drawable.custom_button);
            optionButton.setTextColor(getColor(R.color.button_text_color));
            optionButton.setAllCaps(false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 12);
            optionButton.setLayoutParams(params);

            final int optionIndex = i;
            optionButton.setOnClickListener(v -> {
                dialog.dismiss();
                handlePdfOptionClick(optionIndex, downloadUrl, fileName);
            });

            dialogLayout.addView(optionButton);
        }

        dialog.setContentView(dialogLayout);

        // Make dialog width match parent with margins
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }

    /**
     * ✅ HELPER: Handle PDF option clicks
     */
    private void handlePdfOptionClick(int option, String downloadUrl, String fileName) {
        switch (option) {
            case 0: // Open in Browser
                openPdfInBrowser(downloadUrl, fileName);
                break;
            case 1: // Open in PDF App
                openPdfInApp(downloadUrl, fileName);
                break;
            case 2: // Copy Link
                copyPdfLink(downloadUrl, fileName);
                break;
            case 3: // Cancel
                break;
        }
    }

    /**
     * ✅ BEAUTIFUL: Show beautiful success toast
     */
    private void showBeautifulSuccess(String message) {
        // Create custom toast-like dialog
        Dialog toastDialog = new Dialog(this);
        toastDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        toastDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Create layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setBackgroundResource(R.drawable.custom_button);
        layout.setPadding(24, 16, 24, 16);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Success icon
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_upload);
        icon.setColorFilter(getColor(R.color.button_text_color));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
        iconParams.setMargins(0, 0, 16, 0);
        icon.setLayoutParams(iconParams);
        layout.addView(icon);

        // Message text
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(getColor(R.color.button_text_color));
        text.setTextSize(16);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(text);

        toastDialog.setContentView(layout);

        // Position at top
        Window window = toastDialog.getWindow();
        if (window != null) {
            window.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
            window.getAttributes().y = 100; // Offset from top
        }

        toastDialog.show();

        // Auto dismiss after 3 seconds
        new android.os.Handler().postDelayed(() -> {
            if (toastDialog.isShowing()) {
                toastDialog.dismiss();
            }
        }, 3000);
    }

    /**
     * ✅ BEAUTIFUL: Enhanced error dialog
     */
    private void showBeautifulError(String message) {
        // Create custom error dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Create layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.custom_edittext);
        layout.setPadding(32, 32, 32, 32);

        // Error icon
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_lock);
        icon.setColorFilter(getColor(android.R.color.holo_red_dark));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(64, 64);
        iconParams.setMargins(0, 0, 0, 16);
        iconParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        icon.setLayoutParams(iconParams);
        layout.addView(icon);

        // Error title
        TextView title = new TextView(this);
        title.setText("Error");
        title.setTextColor(getColor(R.color.text_color1));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        layout.addView(title);

        // Error message
        TextView messageText = new TextView(this);
        messageText.setText(message);
        messageText.setTextColor(getColor(R.color.text_color2));
        messageText.setTextSize(16);
        messageText.setGravity(android.view.Gravity.CENTER);
        messageText.setPadding(0, 0, 0, 24);
        layout.addView(messageText);

        // OK button
        Button okButton = new Button(this);
        okButton.setText("OK");
        okButton.setBackgroundResource(R.drawable.custom_button);
        okButton.setTextColor(getColor(R.color.button_text_color));
        okButton.setOnClickListener(v -> dialog.dismiss());
        layout.addView(okButton);

        dialog.setContentView(layout);

        // Make dialog width match parent with margins
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }

    // ✅ UPDATE: Replace these method calls in your existing code:

    /**
     * ✅ UPDATED: Replace showError calls with showBeautifulError
     */
    private void showError(String message) {
        showBeautifulError(message);
        Log.e(TAG, "Error: " + message);
    }

    /**
     * ✅ UPDATED: Replace showSuccess calls with showBeautifulSuccess
     */
    private void showSuccess(String message) {
        showBeautifulSuccess(message);
        Log.d(TAG, "Success: " + message);
    }

    /**
     * ✅ UPDATED: Keep showInfo for simple messages
     */
    private void showInfo(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Select PDF file from device
     */
    private void selectPdfFile() {
        Log.d(TAG, "Starting PDF file selection");

        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            pdfPickerLauncher.launch(Intent.createChooser(intent, "Select PDF File"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to start PDF picker", e);
            showError("Failed to open file picker: " + e.getMessage());
        }
    }

    /**
     * Handle PDF picker result
     */
    private void handlePdfPickerResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            selectedPdfUri = result.getData().getData();

            if (selectedPdfUri != null) {
                Log.d(TAG, "PDF selected: " + selectedPdfUri.toString());
                onPdfSelected();
            } else {
                Log.e(TAG, "Selected PDF URI is null");
                showError("Failed to get selected file");
            }
        } else {
            Log.w(TAG, "PDF selection cancelled or failed");
            showInfo("No file selected");
        }
    }

    /**
     * Handle successful PDF selection
     */
    private void onPdfSelected() {
        selectPdfButton.setText("PDF Selected ✓");

        if (fileSelectedIcon != null) {
            fileSelectedIcon.setVisibility(View.VISIBLE);
        }
        if (fileNotSelectedIcon != null) {
            fileNotSelectedIcon.setVisibility(View.GONE);
        }

        updateUploadButtonState();
        showSuccess("PDF file selected successfully!");

        Log.d(TAG, "PDF selection completed successfully");
    }

    /**
     * Validate inputs and upload PDF
     */
    private void validateAndUploadPdf() {
        String pdfName = pdfNameEditText.getText().toString().trim();

        Log.d(TAG, "Validating upload inputs - PDF name: " + pdfName +
                ", Category: " + selectedCategory + ", Folder: " + selectedFolderName);

        if (selectedPdfUri == null) {
            showError("Please select a PDF file first");
            return;
        }

        if (pdfName.isEmpty()) {
            showError("Please enter a name for the PDF");
            pdfNameEditText.requestFocus();
            return;
        }

        if (!isValidFileName(pdfName)) {
            showError("PDF name can only contain letters, numbers, spaces, hyphens, and underscores");
            pdfNameEditText.requestFocus();
            return;
        }

        if (!pdfName.toLowerCase().endsWith(".pdf")) {
            pdfName += ".pdf";
        }

        startPdfUpload(pdfName);
    }

    /**
     * ✅ UPDATED: Start PDF upload using Firebase Storage
     */
    private void startPdfUpload(String pdfName) {
        Log.d(TAG, "🚀 Starting PDF upload to Firebase Storage");

        setUploadingState(true);

        try {
            // Convert URI to File
            File pdfFile = createFileFromUri(selectedPdfUri, pdfName);

            if (pdfFile == null) {
                onUploadFailure("Failed to prepare file for upload");
                return;
            }

            // ✅ UPDATED: Upload using Firebase Storage
            storageRepository.uploadPDF(pdfFile, pdfName, selectedFolderName, selectedCategory,
                    new FirebaseStorageRepository.UploadCallback() {
                        @Override
                        public void onSuccess(String downloadUrl, String fileName) {
                            Log.d(TAG, "✅ PDF upload successful to Firebase Storage: " + downloadUrl);
                            runOnUiThread(() -> onUploadSuccess(fileName, downloadUrl));
                        }

                        @Override
                        public void onProgress(int progress) {
                            runOnUiThread(() -> updateUploadProgress(progress));
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e(TAG, "❌ Firebase Storage upload failed: " + errorMessage);
                            runOnUiThread(() -> onUploadFailure(errorMessage));
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Failed to start upload", e);
            onUploadFailure("Failed to start upload: " + e.getMessage());
        }
    }

    /**
     * Create File from URI
     */
    private File createFileFromUri(Uri uri, String fileName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file from URI", e);
            return null;
        }
    }

    /**
     * ✅ UPDATED: Handle successful upload with download URL
     */
    private void onUploadSuccess(String fileName, String downloadUrl) {
        setUploadingState(false);
        showSuccess("PDF uploaded successfully to Firebase Storage! 🎉");

        Log.d(TAG, "✅ Upload completed successfully: " + fileName);
        Log.d(TAG, "📁 Download URL: " + downloadUrl);

        // Show success dialog with options
        new AlertDialog.Builder(this)
                .setTitle("Upload Successful!")
                .setMessage("PDF '" + fileName + "' has been uploaded successfully to Firebase Storage.\n\n" +
                        "Students can now access this file in the " +
                        categories[getSelectedCategoryIndex()] + " section.")
                .setPositiveButton("Upload Another", (dialog, which) -> resetUploadForm())
                .setNegativeButton("Go to Home", (dialog, which) -> navigateToHome())
                .setNeutralButton("Test Storage", (dialog, which) -> testFirebaseStorage())
                .show();
    }

    /**
     * ✅ NEW: Test Firebase Storage connection
     */
    private void testFirebaseStorage() {
        Log.d(TAG, "🧪 Testing Firebase Storage connection...");

        storageRepository.testStorageConnection(new FirebaseStorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String result, String message) {
                Log.d(TAG, "✅ Firebase Storage test successful: " + message);
                showSuccess("Firebase Storage is working perfectly! ✅");
            }

            @Override
            public void onProgress(int progress) {
                // Not used for test
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "❌ Firebase Storage test failed: " + errorMessage);
                showError("Firebase Storage test failed: " + errorMessage);
            }
        });
    }

    /**
     * Handle upload failure
     */
    private void onUploadFailure(String errorMessage) {
        setUploadingState(false);
        showError("Upload failed: " + errorMessage);
        Log.e(TAG, "Upload failed: " + errorMessage);
    }

    /**
     * Update upload progress
     */
    private void updateUploadProgress(int progress) {
        if (uploadProgressBar != null) {
            uploadProgressBar.setProgress(progress);
        }
        if (uploadStatusText != null) {
            uploadStatusText.setText("Uploading to Firebase Storage... " + progress + "%");
        }
    }

    /**
     * Set uploading state for UI
     */
    private void setUploadingState(boolean isUploading) {
        selectPdfButton.setEnabled(!isUploading);
        uploadPdfButton.setEnabled(!isUploading);
        pdfNameEditText.setEnabled(!isUploading);

        if (categorySpinner != null) categorySpinner.setEnabled(!isUploading);
        if (folderSpinner != null) folderSpinner.setEnabled(!isUploading);

        if (progressSection != null) {
            progressSection.setVisibility(isUploading ? View.VISIBLE : View.GONE);
        }

        if (!isUploading) {
            uploadPdfButton.setText("Upload PDF");
            if (uploadStatusText != null) {
                uploadStatusText.setText("");
            }
        }
    }

    /**
     * Update upload button state
     */
    private void updateUploadButtonState() {
        boolean canUpload = selectedPdfUri != null &&
                !pdfNameEditText.getText().toString().trim().isEmpty();
        uploadPdfButton.setEnabled(canUpload);
    }

    /**
     * Reset upload form
     */
    private void resetUploadForm() {
        selectedPdfUri = null;
        pdfNameEditText.setText("");
        selectPdfButton.setText("Select PDF");

        if (fileSelectedIcon != null) {
            fileSelectedIcon.setVisibility(View.GONE);
        }
        if (fileNotSelectedIcon != null) {
            fileNotSelectedIcon.setVisibility(View.VISIBLE);
        }

        uploadPdfButton.setText("Upload PDF");
        uploadPdfButton.setEnabled(false);

        categorySpinner.setSelection(0);
        folderSpinner.setSelection(0);

        selectedFolderName = "";

        if (progressSection != null) {
            progressSection.setVisibility(View.GONE);
        }
    }

    // Helper methods
    private boolean isValidFileName(String fileName) {
        return fileName.matches("^[a-zA-Z0-9\\s\\-_]+$");
    }

    private int getSelectedCategoryIndex() {
        for (int i = 0; i < categoryKeys.length; i++) {
            if (categoryKeys[i].equals(selectedCategory)) {
                return i;
            }
        }
        return 0;
    }

    // Navigation methods
    private void navigateToHome() {
        Intent intent = new Intent(UploadActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(UploadActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // User feedback methods
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showInfo(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Activity lifecycle
    @Override
    protected void onStart() {
        super.onStart();
        if (!authRepository.isUserSignedIn()) {
            navigateToSignIn();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "✅ UploadActivity destroyed - Firebase Storage migration complete!");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToHome();
    }
}
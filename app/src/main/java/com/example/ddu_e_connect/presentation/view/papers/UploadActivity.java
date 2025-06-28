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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.data.source.remote.GoogleAuthRepository;
import com.example.ddu_e_connect.data.source.remote.GoogleDriveRepository;
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
    private static final int DRIVE_AUTHORIZATION_REQUEST_CODE = 1001;

    // UI Components
    private ActivityUploadBinding binding;
    private Spinner categorySpinner;
    private Spinner folderSpinner;
    private EditText pdfNameEditText;
    private EditText newFolderEditText;
    private Button selectPdfButton;
    private Button uploadPdfButton;
    private Button createFolderButton;
    private ProgressBar uploadProgressBar;
    private TextView uploadStatusText;

    // Data and Services
    private GoogleAuthRepository authRepository;
    private GoogleDriveRepository driveRepository;
    private GoogleSignInAccount currentUser;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;


    private ImageView fileSelectedIcon;
    private ImageView fileNotSelectedIcon;
    private View progressSection;

    // Upload Data
    private Uri selectedPdfUri;
    private String selectedCategory = "academic";
    private String selectedFolderId = null;
    private String selectedFolderName = "";
    private List<GoogleDriveRepository.DriveFolder> availableFolders = new ArrayList<>();

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

        Log.d(TAG, "Enhanced UploadActivity initialized");
    }

    /**
     * Initialize components and services
     */
    private void initializeComponents() {
        // Initialize repositories
        authRepository = new GoogleAuthRepository(this);
        driveRepository = new GoogleDriveRepository(this);
        currentUser = authRepository.getCurrentUser();

        // Initialize UI components
        initializeUIComponents();

        // Setup PDF picker launcher
        setupPdfPickerLauncher();
    }

    /**
     * Initialize UI components - FIXED VERSION
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

        // Spinners - ASSIGN TO CLASS FIELDS
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
     * Setup category and folder spinners
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
                loadFoldersForCategory();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup folder spinner (will be populated when category is selected)
        updateFolderSpinner();

        folderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // "Select Folder" option
                    selectedFolderId = null;
                    selectedFolderName = "";
                } else if (position == availableFolders.size() + 1) {
                    // "Create New Folder" option
                    showCreateFolderDialog();
                } else {
                    // Existing folder selected
                    GoogleDriveRepository.DriveFolder selectedFolder = availableFolders.get(position - 1);
                    selectedFolderId = selectedFolder.getId();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle Google Drive authorization result
        if (driveRepository != null) {
            driveRepository.handleAuthorizationResult(requestCode, resultCode);
        }
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

        // Load initial folders (this will be called after spinners are set up)
        if (driveRepository != null) {
            loadFoldersForCategory();
        }

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
     * ENHANCED: Check if user has upload permissions with detailed logging
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

                // Try to re-assign role based on email as fallback
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
     * Enhanced method to handle role-based permissions
     */
    private void handleRolePermissions(String role) {
        boolean canUpload = "admin".equalsIgnoreCase(role) || "helper".equalsIgnoreCase(role);

        if (!canUpload) {
            Log.w(TAG, "User does not have upload permissions. Role: " + role);
            showUnauthorizedDialog(role);
            disableUploadFeatures();
        } else {
            Log.d(TAG, "User has upload permissions. Role: " + role);
            enableUploadFeatures(role);
            showWelcomeMessage(role);
        }
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
     * ENHANCED: Show detailed unauthorized dialog
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
                    // Redirect to papers activity where they can view PDFs
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
            String adminEmail = "mrvivekthumar@gmail.com"; // Primary admin from your contacts
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

            // Fallback: Navigate to contact activity
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

        // User information
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
     * Show detailed role information dialog
     */
    private void showRoleInformation() {
        String title = "📋 Role Information";

        String roleInfo = "🏛️ DDU E-Connect Role System:\n\n" +

                "👑 ADMIN Role:\n" +
                "• Full access to upload PDFs\n" +
                "• Create and manage folders\n" +
                "• Access all app features\n" +
                "• Manage user permissions\n\n" +

                "🤝 HELPER Role:\n" +
                "• Upload PDFs to help students\n" +
                "• Create folders in categories\n" +
                "• Usually for faculty/teachers\n" +
                "• Institutional email preferred\n\n" +

                "👨‍🎓 STUDENT Role:\n" +
                "• Access all study materials\n" +
                "• Download exam papers\n" +
                "• Browse club documents\n" +
                "• Cannot upload files\n\n" +

                "🔄 Role Assignment:\n" +
                "• Automatic based on email\n" +
                "• @ddu.ac.in emails get HELPER role\n" +
                "• Predefined admin emails get ADMIN\n" +
                "• All others get STUDENT role\n\n" +

                "📧 Need role change? Contact admin!";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(roleInfo)
                .setPositiveButton("Got it!", null)
                .setNeutralButton("Contact Admin", (dialog, which) -> contactAdminForPermission())
                .show();
    }

    /**
     * Enable upload features for authorized users
     */
    private void enableUploadFeatures(String role) {
        selectPdfButton.setEnabled(true);
        pdfNameEditText.setEnabled(true);
        categorySpinner.setEnabled(true);
        folderSpinner.setEnabled(true);

        // Show welcome message
        showSuccess("Welcome " + role.toUpperCase() + "! You can upload PDFs to help students.");

        // Request Drive permission
        driveRepository.requestDrivePermission(new GoogleDriveRepository.AuthCallback() {
            @Override
            public void onAuthSuccess() {
                Log.d(TAG, "Drive permission granted");
                loadFoldersForCategory();
            }

            @Override
            public void onAuthFailure(String errorMessage) {
                Log.e(TAG, "Drive permission failed: " + errorMessage);
                showError("Google Drive permission required for uploads");
            }
        });
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
     * Load folders for selected category
     */
    private void loadFoldersForCategory() {
        Log.d(TAG, "Loading folders for category: " + selectedCategory);

        driveRepository.getFoldersInCategory(selectedCategory, new GoogleDriveRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<GoogleDriveRepository.DriveFolder> folders) {
                Log.d(TAG, "Loaded " + folders.size() + " folders");
                availableFolders.clear();
                availableFolders.addAll(folders);
                updateFolderSpinner();
            }

            @Override
            public void onFolderCreated(String folderId, String folderName) {
                // Reload folders after creation
                loadFoldersForCategory();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load folders: " + errorMessage);
                showError("Failed to load folders: " + errorMessage);
            }
        });
    }

    /**
     * Update folder spinner with available folders
     */
    private void updateFolderSpinner() {
        List<String> folderNames = new ArrayList<>();
        folderNames.add("Select Folder (Optional)");

        for (GoogleDriveRepository.DriveFolder folder : availableFolders) {
            folderNames.add(folder.getName());
        }

        folderNames.add("+ Create New Folder");

        ArrayAdapter<String> folderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, folderNames);
        folderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderSpinner.setAdapter(folderAdapter);
    }

    /**
     * Show create folder dialog
     */
    private void showCreateFolderDialog() {
        EditText folderNameInput = new EditText(this);
        folderNameInput.setHint("Enter folder name");

        new AlertDialog.Builder(this)
                .setTitle("Create New Folder")
                .setMessage("Create a new folder in " + categories[getSelectedCategoryIndex()])
                .setView(folderNameInput)
                .setPositiveButton("Create", (dialog, which) -> {
                    String folderName = folderNameInput.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        createFolder(folderName);
                    } else {
                        showError("Please enter a folder name");
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Reset spinner selection
                    folderSpinner.setSelection(0);
                })
                .show();
    }

    /**
     * Create new folder
     */
    private void createFolder(String folderName) {
        Log.d(TAG, "Creating folder: " + folderName + " in category: " + selectedCategory);

        driveRepository.createFolder(folderName, selectedCategory, new GoogleDriveRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<GoogleDriveRepository.DriveFolder> folders) {
                // Not used
            }

            @Override
            public void onFolderCreated(String folderId, String folderName) {
                Log.d(TAG, "Folder created successfully: " + folderName);
                showSuccess("Folder '" + folderName + "' created successfully!");

                // Set as selected folder
                selectedFolderId = folderId;
                selectedFolderName = folderName;

                // Reload folders
                loadFoldersForCategory();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to create folder: " + errorMessage);
                showError("Failed to create folder: " + errorMessage);
                folderSpinner.setSelection(0);
            }
        });
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

        // Show/hide file icons
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

        // Validate inputs
        if (selectedPdfUri == null) {
            showError("Please select a PDF file first");
            return;
        }

        if (pdfName.isEmpty()) {
            showError("Please enter a name for the PDF");
            pdfNameEditText.requestFocus();
            return;
        }

        // Validate PDF name
        if (!isValidFileName(pdfName)) {
            showError("PDF name can only contain letters, numbers, spaces, hyphens, and underscores");
            pdfNameEditText.requestFocus();
            return;
        }

        // Add .pdf extension if not present
        if (!pdfName.toLowerCase().endsWith(".pdf")) {
            pdfName += ".pdf";
        }

        // Start upload
        startPdfUpload(pdfName);
    }

    /**
     * Start PDF upload process
     */
    private void startPdfUpload(String pdfName) {
        Log.d(TAG, "Starting PDF upload process");

        setUploadingState(true);

        try {
            // Convert URI to File
            File pdfFile = createFileFromUri(selectedPdfUri, pdfName);

            if (pdfFile == null) {
                onUploadFailure("Failed to prepare file for upload");
                return;
            }

            // Upload using Google Drive API
            driveRepository.uploadPDF(pdfFile, pdfName, selectedFolderName, selectedCategory,
                    new GoogleDriveRepository.UploadCallback() {
                        @Override
                        public void onSuccess(String fileId, String fileName) {
                            Log.d(TAG, "PDF upload successful: " + fileId);
                            runOnUiThread(() -> onUploadSuccess(fileName));
                        }

                        @Override
                        public void onProgress(int progress) {
                            runOnUiThread(() -> updateUploadProgress(progress));
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e(TAG, "PDF upload failed: " + errorMessage);
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
     * Handle successful upload
     */
    private void onUploadSuccess(String fileName) {
        setUploadingState(false);
        showSuccess("PDF uploaded successfully! 🎉");

        Log.d(TAG, "Upload completed successfully: " + fileName);

        // Show success dialog with options
        new AlertDialog.Builder(this)
                .setTitle("Upload Successful!")
                .setMessage("PDF '" + fileName + "' has been uploaded successfully.\n\n" +
                        "Students can now access this file in the " +
                        categories[getSelectedCategoryIndex()] + " section.")
                .setPositiveButton("Upload Another", (dialog, which) -> resetUploadForm())
                .setNegativeButton("Go to Home", (dialog, which) -> navigateToHome())
                .show();
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
            uploadStatusText.setText("Uploading... " + progress + "%");
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

        // Reset file selection icons
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

        selectedFolderId = null;
        selectedFolderName = "";

        // Hide progress section
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
        Log.d(TAG, "Enhanced UploadActivity destroyed");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToHome();
    }

    /**
     * DEBUG: Test current user role (add this to any activity for testing)
     */
    private void debugUserRole() {
        GoogleSignInAccount user = authRepository.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "DEBUG: No user signed in");
            return;
        }

        authRepository.fetchUserRole(user.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                String debugInfo = "🔍 ROLE DEBUG:\n\n" +
                        "👤 User: " + user.getDisplayName() + "\n" +
                        "📧 Email: " + user.getEmail() + "\n" +
                        "🔐 Role: " + role.toUpperCase() + "\n\n" +
                        "Permissions:\n" +
                        "📤 Can Upload: " + (RoleManager.canUpload(role) ? "✅ YES" : "❌ NO") + "\n" +
                        "👑 Is Admin: " + (RoleManager.isAdmin(role) ? "✅ YES" : "❌ NO") + "\n" +
                        "🤝 Is Helper: " + (RoleManager.isHelper(role) ? "✅ YES" : "❌ NO");

                Log.d(TAG, debugInfo);

                new AlertDialog.Builder(UploadActivity.this)
                        .setTitle("🔍 Role Debug")
                        .setMessage(debugInfo)
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "DEBUG: Role fetch failed: " + errorMessage);
            }
        });
    }
}
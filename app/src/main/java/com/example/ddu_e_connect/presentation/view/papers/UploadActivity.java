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
     * Initialize UI components
     */
    private void initializeUIComponents() {
        // Find UI components
        pdfNameEditText = binding.pdfNameEditText;
        selectPdfButton = binding.selectPdfButton;
        uploadPdfButton = binding.uploadPdfButton;

        // Create additional UI components programmatically
        setupCategorySpinner();
        setupFolderSpinner();
        setupProgressComponents();
        setupNewFolderComponents();
    }

    /**
     * Setup category spinner
     */
    private void setupCategorySpinner() {
        // Add category spinner above PDF name field
        categorySpinner = new Spinner(this);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = categoryKeys[position];
                loadFoldersForCategory();
                Log.d(TAG, "Category selected: " + selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Add to layout (you'll need to update your XML)
    }

    /**
     * Setup folder spinner
     */
    private void setupFolderSpinner() {
        folderSpinner = new Spinner(this);
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
     * Setup progress components
     */
    private void setupProgressComponents() {
        uploadProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        uploadProgressBar.setVisibility(View.GONE);
        uploadProgressBar.setMax(100);

        uploadStatusText = new TextView(this);
        uploadStatusText.setVisibility(View.GONE);
        uploadStatusText.setTextColor(getResources().getColor(R.color.text_color2));
    }

    /**
     * Setup new folder components
     */
    private void setupNewFolderComponents() {
        newFolderEditText = new EditText(this);
        newFolderEditText.setHint("Enter new folder name");
        newFolderEditText.setVisibility(View.GONE);

        createFolderButton = new Button(this);
        createFolderButton.setText("Create Folder");
        createFolderButton.setVisibility(View.GONE);

        createFolderButton.setOnClickListener(v -> {
            String folderName = newFolderEditText.getText().toString().trim();
            if (!folderName.isEmpty()) {
                createFolder(folderName);
            } else {
                showError("Please enter a folder name");
            }
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

        // Load initial folders
        loadFoldersForCategory();
    }

    /**
     * Check if user has upload permissions
     */
    private void checkUserPermissions() {
        if (currentUser == null) {
            Log.w(TAG, "No user signed in");
            showError("You must be signed in to upload files");
            navigateToSignIn();
            return;
        }

        Log.d(TAG, "Checking upload permissions for user: " + currentUser.getEmail());

        authRepository.fetchUserRole(currentUser.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                Log.d(TAG, "User role: " + role);
                handleRolePermissions(role);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to fetch user role: " + errorMessage);
                showError("Failed to verify permissions: " + errorMessage);
                disableUploadFeatures();
            }
        });
    }

    /**
     * Handle role-based permissions
     */
    private void handleRolePermissions(String role) {
        boolean canUpload = "admin".equalsIgnoreCase(role) || "helper".equalsIgnoreCase(role);

        if (!canUpload) {
            Log.w(TAG, "User does not have upload permissions. Role: " + role);
            showUnauthorizedDialog(role);
            disableUploadFeatures();
        } else {
            Log.d(TAG, "User has upload permissions");
            enableUploadFeatures(role);
        }
    }

    /**
     * Show unauthorized access dialog
     */
    private void showUnauthorizedDialog(String userRole) {
        new AlertDialog.Builder(this)
                .setTitle("Upload Permission Required")
                .setMessage("You don't have permission to upload files.\n\n" +
                        "Your current role: " + userRole.toUpperCase() + "\n" +
                        "Required roles: ADMIN or HELPER\n\n" +
                        "Please contact an administrator to get upload permissions.")
                .setPositiveButton("Contact Admin", (dialog, which) -> {
                    // Navigate to contact activity
                    startActivity(new Intent(this,
                            com.example.ddu_e_connect.presentation.view.contact.ContactUsActivity.class));
                })
                .setNegativeButton("Go Back", (dialog, which) -> {
                    navigateToHome();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Enable upload features for authorized users
     */
    private void enableUploadFeatures(String role) {
        selectPdfButton.setEnabled(true);
        pdfNameEditText.setEnabled(true);

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
        selectPdfButton.setBackgroundColor(getResources().getColor(R.color.button_background));

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
        categorySpinner.setEnabled(!isUploading);
        folderSpinner.setEnabled(!isUploading);

        if (uploadProgressBar != null) {
            uploadProgressBar.setVisibility(isUploading ? View.VISIBLE : View.GONE);
        }
        if (uploadStatusText != null) {
            uploadStatusText.setVisibility(isUploading ? View.VISIBLE : View.GONE);
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
        selectPdfButton.setBackgroundColor(getResources().getColor(R.color.EditText_background));
        uploadPdfButton.setText("Upload PDF");
        uploadPdfButton.setEnabled(false);
        categorySpinner.setSelection(0);
        folderSpinner.setSelection(0);
        selectedFolderId = null;
        selectedFolderName = "";
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
}
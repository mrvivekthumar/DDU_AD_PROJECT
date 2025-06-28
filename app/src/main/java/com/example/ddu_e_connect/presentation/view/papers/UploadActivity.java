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
import android.widget.ScrollView;
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
import com.example.ddu_e_connect.data.source.remote.FirebaseStorageRepository;
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

    // CORRECTED: Services - Use your actual repository classes
    private GoogleAuthRepository authRepository;
    private FirebaseStorageRepository storageRepository;
    private GoogleSignInAccount currentUser;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    // Upload Data
    private Uri selectedPdfUri;
    private String selectedCategory = "academic";
    private String selectedFolderName = "";
    private List<FirebaseStorageRepository.StorageFolder> availableFolders = new ArrayList<>();

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
        addDebugButton();
        checkUserPermissions();

        Log.d(TAG, "✅ UploadActivity initialized with Firebase Storage");

        addTestButton();


    }

    private void addTestButton() {
        try {
            ScrollView scrollView = (ScrollView) binding.getRoot();
            LinearLayout mainContainer = (LinearLayout) scrollView.getChildAt(0);

            Button testButton = new Button(this);
            testButton.setText("🧪 Test Upload Now");
            testButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            testButton.setTextColor(getResources().getColor(android.R.color.white));
            testButton.setOnClickListener(v -> {
                Log.d(TAG, "🧪 TEST BUTTON CLICKED");
                showInfo("Test button clicked - check logs");

                // Show current state
                Log.d(TAG, "Current state:");
                Log.d(TAG, "- Selected URI: " + selectedPdfUri);
                Log.d(TAG, "- PDF Name: " + pdfNameEditText.getText().toString());
                Log.d(TAG, "- Category: " + selectedCategory);
                Log.d(TAG, "- Current User: " + (currentUser != null ? currentUser.getEmail() : "null"));

                // Test Firebase connection
                testFirebaseConnection();
            });

            mainContainer.addView(testButton);
            Log.d(TAG, "✅ Test button added");

        } catch (Exception e) {
            Log.e(TAG, "Failed to add test button: " + e.getMessage());
        }
    }
    /**
     * TEST FIREBASE CONNECTION
     */
    private void testFirebaseConnection() {
        Log.d(TAG, "🧪 Testing Firebase connection...");

        storageRepository.testStorageConnection(new FirebaseStorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String result, String message) {
                Log.d(TAG, "✅ Firebase test SUCCESS: " + message);
                showSuccess("Firebase connected! ✅");
            }

            @Override
            public void onProgress(int progress) {
                // Not used
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "❌ Firebase test FAILED: " + errorMessage);
                showError("Firebase connection failed: " + errorMessage);
            }
        });
    }


    /**
     * CORRECTED: Initialize components with correct constructors
     */
    private void initializeComponents() {
        // Initialize repositories with correct types and constructors
        authRepository = new GoogleAuthRepository(this);
        storageRepository = new FirebaseStorageRepository(this);  // Pass context
        currentUser = authRepository.getCurrentUser();

        // Initialize UI components
        initializeUIComponents();

        // Initialize PDF picker launcher
        initializePdfPickerLauncher();
    }

    /**
     * Initialize UI components from binding
     */
    private void initializeUIComponents() {
        categorySpinner = binding.categorySpinner;
        folderSpinner = binding.folderSpinner;
        pdfNameEditText = binding.pdfNameEditText;
        selectPdfButton = binding.selectPdfButton;
        uploadPdfButton = binding.uploadPdfButton;
        uploadProgressBar = binding.uploadProgressBar;
        uploadStatusText = binding.uploadStatusText;
        fileSelectedIcon = binding.fileSelectedIcon;
        fileNotSelectedIcon = binding.fileNotSelectedIcon;
        progressSection = binding.progressSection;

        Log.d(TAG, "✅ UI components initialized");
    }

    /**
     * Initialize PDF picker launcher
     */
    private void initializePdfPickerLauncher() {
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
        // Setup category spinner
        setupCategorySpinner();

        // Setup folder spinner (initially empty)
        setupFolderSpinner();

        // Select PDF button
        selectPdfButton.setOnClickListener(v -> selectPdfFile());

        // Upload PDF button
        uploadPdfButton.setOnClickListener(v -> validateAndUploadPdf());

        // Initially disable upload button
        updateUploadButtonState();

        // Load initial folders from Firebase Storage
        loadFoldersForCategory();

        setupTextInputListeners();
    }

    /**
     * Setup category spinner
     */
    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

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
    }

    /**
     * Setup folder spinner
     */
    private void setupFolderSpinner() {
        // Initial empty adapter
        String[] initialFolders = {"Select folder..."};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, initialFolders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderSpinner.setAdapter(adapter);

        folderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select folder..." option
                    selectedFolderName = parent.getItemAtPosition(position).toString();
                    updateUploadButtonState();
                    Log.d(TAG, "Folder selected: " + selectedFolderName);
                } else {
                    selectedFolderName = "";
                    updateUploadButtonState();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
     * CORRECTED: Check user permissions using GoogleAuthRepository
     */
    private void checkUserPermissions() {
        if (currentUser == null) {
            Log.w(TAG, "No user signed in");
            showUnauthorizedDialog("unknown");
            navigateToSignIn();
            return;
        }

        Log.d(TAG, "Checking upload permissions for user: " + currentUser.getEmail());

        // CORRECTED: Use GoogleAuthRepository.RoleCallback
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
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to fetch user role: " + errorMessage);
                showError("Failed to check permissions: " + errorMessage);
                showUnauthorizedDialog("unknown");
            }
        });
    }

    /**
     * Enable upload features for authorized users
     */
    private void enableUploadFeatures(String role) {
        selectPdfButton.setEnabled(true);
        uploadPdfButton.setEnabled(false); // Will be enabled when file is selected
        pdfNameEditText.setEnabled(true);

        selectPdfButton.setText("Select PDF");
        uploadPdfButton.setText("Upload PDF");

        showInfo("Upload access granted! Firebase Storage ready for uploads.");

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
     * BYPASS FOLDER LOADING - Use simple default setup
     * Replace your loadFoldersForCategory() method with this:
     */
    private void loadFoldersForCategory() {
        Log.d(TAG, "📁 Setting up simple folder structure");

        // Create simple folder list
        runOnUiThread(() -> {
            List<String> folderNames = new ArrayList<>();
            folderNames.add("Select folder...");
            folderNames.add("general");
            folderNames.add("assignments");
            folderNames.add("notes");
            folderNames.add("exams");

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, folderNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            folderSpinner.setAdapter(adapter);

            Log.d(TAG, "✅ Simple folder spinner set up");
        });
    }


    /**
     * Update folder spinner with loaded folders
     */
    private void updateFolderSpinner() {
        runOnUiThread(() -> {
            List<String> folderNames = new ArrayList<>();
            folderNames.add("Select folder...");

            for (FirebaseStorageRepository.StorageFolder folder : availableFolders) {
                folderNames.add(folder.getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, folderNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            folderSpinner.setAdapter(adapter);

            Log.d(TAG, "✅ Folder spinner updated with " + (folderNames.size() - 1) + " folders");
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

        if (fileSelectedIcon != null) {
            fileSelectedIcon.setVisibility(View.VISIBLE);
        }
        if (fileNotSelectedIcon != null) {
            fileNotSelectedIcon.setVisibility(View.GONE);
        }

        updateUploadButtonState();
        showSuccess("PDF file selected successfully!");
    }

    /**
     * CORRECTED: Start PDF upload using correct method name and callback
     */
    private void startPdfUpload(String pdfName) {
        Log.d(TAG, "🚀 Starting PDF upload to Firebase Storage");

        setUploadingState(true);

        try {
            // Convert URI to File (required by your FirebaseStorageRepository)
            File pdfFile = createFileFromUri(selectedPdfUri, pdfName);

            if (pdfFile == null) {
                onUploadFailure("Failed to prepare file for upload");
                return;
            }

            // CORRECTED: Use uploadPDF method with correct callback signature
            storageRepository.uploadPDF(pdfFile, pdfName, selectedFolderName, selectedCategory,
                    new FirebaseStorageRepository.UploadCallback() {
                        @Override
                        public void onSuccess(String downloadUrl, String fileName) {  // Correct signature
                            Log.d(TAG, "✅ PDF upload successful: " + downloadUrl);
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
     * Create File from URI (needed because FirebaseStorageRepository expects File)
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

            Log.d(TAG, "✅ File created from URI: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file from URI", e);
            return null;
        }
    }

    /**
     * Handle successful upload with download URL
     */
    private void onUploadSuccess(String fileName, String downloadUrl) {
        setUploadingState(false);
        showSuccess("PDF uploaded successfully! 🎉");

        Log.d(TAG, "✅ Upload completed successfully: " + fileName);
        Log.d(TAG, "📁 Download URL: " + downloadUrl);

        // Show success dialog with options
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload Successful!")
                .setMessage("PDF '" + fileName + "' has been uploaded successfully!\n\n" +
                        "Students can now access this file in the " +
                        categories[getSelectedCategoryIndex()] + " section.")
                .setPositiveButton("Upload Another", (dialog, which) -> {
                    resetUploadForm();
                    showSuccess("Ready for another upload!");
                })
                .setNegativeButton("Go Home", (dialog, which) -> navigateToHome())
                .setNeutralButton("View Papers", (dialog, which) -> {
                    Intent intent = new Intent(this, PapersActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Show unauthorized dialog
     */
    private void showUnauthorizedDialog(String userRole) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Access Restricted")
                .setMessage("You don't have permission to upload files.\n\nYour current role: " +
                        userRole + "\n\nContact an administrator for upload access.")
                .setPositiveButton("Contact Admin", (dialog, which) -> contactAdminForPermission())
                .setNegativeButton("View Papers", (dialog, which) -> {
                    Intent intent = new Intent(this, PapersActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNeutralButton("Go Home", (dialog, which) -> navigateToHome())
                .setCancelable(false)
                .show();

        Log.d(TAG, "✅ Unauthorized dialog shown for role: " + userRole);
    }

    /**
     * Contact admin for permission
     */
    private void contactAdminForPermission() {
        showInfo("Please contact your administrator for upload permissions.");
        // TODO: Implement actual contact functionality
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
     * SIMPLIFIED BUTTON STATE - Remove folder dependency
     * Replace your updateUploadButtonState() method with this:
     */
    private void updateUploadButtonState() {
        boolean canUpload = selectedPdfUri != null &&
                !pdfNameEditText.getText().toString().trim().isEmpty();
        uploadPdfButton.setEnabled(canUpload);

        Log.d(TAG, "📋 Upload button state: " + (canUpload ? "ENABLED" : "DISABLED"));
        Log.d(TAG, "   - PDF selected: " + (selectedPdfUri != null));
        Log.d(TAG, "   - Name entered: " + (!pdfNameEditText.getText().toString().trim().isEmpty()));
    }

    /**
     * DEBUGGING VERSION - Add these methods to your UploadActivity.java
     * This will help us identify exactly where the upload is failing
     */

// Add this method to debug the complete upload flow
    private void debugUploadFlow() {
        Log.d(TAG, "🔍 DEBUGGING UPLOAD FLOW:");
        Log.d(TAG, "Selected PDF URI: " + selectedPdfUri);
        Log.d(TAG, "PDF Name: " + pdfNameEditText.getText().toString().trim());
        Log.d(TAG, "Selected Category: " + selectedCategory);
        Log.d(TAG, "Selected Folder: " + selectedFolderName);
        Log.d(TAG, "Current User: " + (currentUser != null ? currentUser.getEmail() : "null"));

        // Test Firebase connection first
        testFirebaseStorageConnection();
    }

    // Add this method to test Firebase Storage connection
    private void testFirebaseStorageConnection() {
        Log.d(TAG, "🧪 Testing Firebase Storage connection...");

        storageRepository.testStorageConnection(new FirebaseStorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String result, String message) {
                Log.d(TAG, "✅ Firebase Storage test successful: " + message);
                showSuccess("Firebase Storage connected! Now testing upload...");

                // If connection works, proceed with actual upload
                proceedWithDebugUpload();
            }

            @Override
            public void onProgress(int progress) {
                // Not used for connection test
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "❌ Firebase Storage connection failed: " + errorMessage);
                showError("Firebase Storage connection failed: " + errorMessage);

                // Show detailed error dialog
                new AlertDialog.Builder(UploadActivity.this)
                        .setTitle("❌ Firebase Storage Error")
                        .setMessage("Connection failed:\n" + errorMessage +
                                "\n\nPossible issues:" +
                                "\n• Firebase Storage rules" +
                                "\n• Network connectivity" +
                                "\n• Firebase project configuration")
                        .setPositiveButton("Retry", (dialog, which) -> testFirebaseStorageConnection())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    // Add this method to proceed with debug upload
    private void proceedWithDebugUpload() {
        String pdfName = pdfNameEditText.getText().toString().trim();

        if (!pdfName.toLowerCase().endsWith(".pdf")) {
            pdfName += ".pdf";
        }

        Log.d(TAG, "🚀 Proceeding with debug upload: " + pdfName);

        // Step 1: Test file creation
        debugFileCreation(pdfName);
    }

    // Add this method to debug file creation
    private void debugFileCreation(String pdfName) {
        Log.d(TAG, "🔍 Step 1: Testing file creation from URI");

        try {
            File pdfFile = createFileFromUri(selectedPdfUri, pdfName);

            if (pdfFile == null) {
                Log.e(TAG, "❌ File creation failed - returned null");
                showError("Failed to create file from selected PDF");
                return;
            }

            if (!pdfFile.exists()) {
                Log.e(TAG, "❌ File creation failed - file doesn't exist");
                showError("Created file doesn't exist");
                return;
            }

            Log.d(TAG, "✅ File created successfully:");
            Log.d(TAG, "   File path: " + pdfFile.getAbsolutePath());
            Log.d(TAG, "   File size: " + pdfFile.length() + " bytes");
            Log.d(TAG, "   File readable: " + pdfFile.canRead());

            if (pdfFile.length() == 0) {
                Log.e(TAG, "❌ File is empty!");
                showError("Created file is empty");
                return;
            }

            showSuccess("File created successfully (" + pdfFile.length() + " bytes)");

            // Step 2: Test upload path generation
            debugUploadPath(pdfFile, pdfName);

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in file creation: " + e.getMessage(), e);
            showError("File creation exception: " + e.getMessage());
        }
    }

    // Add this method to debug upload path
    private void debugUploadPath(File pdfFile, String pdfName) {
        Log.d(TAG, "🔍 Step 2: Testing upload path generation");

        // Calculate what the path should be
        String expectedPath = "ddu_papers/" + selectedCategory;
        if (selectedFolderName != null && !selectedFolderName.isEmpty()) {
            expectedPath += "/" + selectedFolderName;
        }
        expectedPath += "/" + pdfName;

        Log.d(TAG, "Expected storage path: " + expectedPath);

        showSuccess("Upload path: " + expectedPath);

        // Step 3: Start actual upload with detailed logging
        debugActualUpload(pdfFile, pdfName);
    }

    // Add this method to debug actual upload
    private void debugActualUpload(File pdfFile, String pdfName) {
        Log.d(TAG, "🔍 Step 3: Starting actual upload with detailed logging");

        setUploadingState(true);

        storageRepository.uploadPDF(pdfFile, pdfName, selectedFolderName, selectedCategory,
                new FirebaseStorageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl, String fileName) {
                        Log.d(TAG, "🎉 UPLOAD SUCCESS!");
                        Log.d(TAG, "   File name: " + fileName);
                        Log.d(TAG, "   Download URL: " + downloadUrl);

                        runOnUiThread(() -> {
                            setUploadingState(false);
                            showSuccess("Upload successful! File: " + fileName);

                            // Show detailed success dialog
                            new AlertDialog.Builder(UploadActivity.this)
                                    .setTitle("🎉 Upload Successful!")
                                    .setMessage("File: " + fileName +
                                            "\nCategory: " + selectedCategory +
                                            "\nFolder: " + selectedFolderName +
                                            "\nDownload URL: " + downloadUrl)
                                    .setPositiveButton("Great!", null)
                                    .show();

                            resetUploadForm();
                        });
                    }

                    @Override
                    public void onProgress(int progress) {
                        Log.d(TAG, "📈 Upload progress: " + progress + "%");
                        runOnUiThread(() -> {
                            updateUploadProgress(progress);
                            if (progress % 25 == 0) { // Show message every 25%
                                showInfo("Uploading... " + progress + "%");
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "💥 UPLOAD FAILED!");
                        Log.e(TAG, "   Error: " + errorMessage);

                        runOnUiThread(() -> {
                            setUploadingState(false);

                            // Show detailed error dialog
                            new AlertDialog.Builder(UploadActivity.this)
                                    .setTitle("❌ Upload Failed")
                                    .setMessage("Error: " + errorMessage +
                                            "\n\nDebug info:" +
                                            "\nFile: " + pdfName +
                                            "\nCategory: " + selectedCategory +
                                            "\nFolder: " + selectedFolderName +
                                            "\nFile size: " + pdfFile.length() + " bytes")
                                    .setPositiveButton("Retry", (dialog, which) -> debugUploadFlow())
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    }
                });
    }

    /**
     * SIMPLE WORKING UPLOAD SOLUTION
     * Replace your existing validateAndUploadPdf() method with this:
     */
    private void validateAndUploadPdf() {
        String pdfName = pdfNameEditText.getText().toString().trim();

        Log.d(TAG, "🚀 STARTING SIMPLE UPLOAD");
        Log.d(TAG, "PDF Name: " + pdfName);
        Log.d(TAG, "Selected PDF URI: " + selectedPdfUri);
        Log.d(TAG, "Category: " + selectedCategory);

        // Basic validation
        if (selectedPdfUri == null) {
            showError("Please select a PDF file first");
            Log.e(TAG, "❌ No PDF selected");
            return;
        }

        if (pdfName.isEmpty()) {
            showError("Please enter a name for the PDF");
            Log.e(TAG, "❌ No PDF name entered");
            return;
        }

        if (!pdfName.toLowerCase().endsWith(".pdf")) {
            pdfName += ".pdf";
        }

        // BYPASS FOLDER SELECTION - Use default folder
        String defaultFolder = "general"; // Simple default folder
        Log.d(TAG, "Using default folder: " + defaultFolder);

        // Start simple upload
        startSimpleUpload(pdfName, defaultFolder);
    }

    private void startSimpleUpload(String pdfName, String folderName) {
        Log.d(TAG, "🚀 Starting simple upload: " + pdfName);

        setUploadingState(true);
        showInfo("Starting upload...");

        try {
            // Create file from URI
            File pdfFile = createFileFromUri(selectedPdfUri, pdfName);

            if (pdfFile == null) {
                Log.e(TAG, "❌ Failed to create file");
                setUploadingState(false);
                showError("Failed to prepare file for upload");
                return;
            }

            Log.d(TAG, "✅ File created: " + pdfFile.getAbsolutePath() + " (Size: " + pdfFile.length() + " bytes)");

            // Upload to Firebase Storage
            storageRepository.uploadPDF(pdfFile, pdfName, folderName, selectedCategory,
                    new FirebaseStorageRepository.UploadCallback() {
                        @Override
                        public void onSuccess(String downloadUrl, String fileName) {
                            Log.d(TAG, "🎉 UPLOAD SUCCESS!");
                            Log.d(TAG, "Download URL: " + downloadUrl);
                            Log.d(TAG, "File Name: " + fileName);

                            runOnUiThread(() -> {
                                setUploadingState(false);
                                showSuccess("Upload successful! 🎉");

                                // Simple success dialog
                                new AlertDialog.Builder(UploadActivity.this)
                                        .setTitle("🎉 Success!")
                                        .setMessage("PDF uploaded successfully!\n\nFile: " + fileName)
                                        .setPositiveButton("Upload Another", (dialog, which) -> resetUploadForm())
                                        .setNegativeButton("Done", (dialog, which) -> navigateToHome())
                                        .show();
                            });
                        }

                        @Override
                        public void onProgress(int progress) {
                            Log.d(TAG, "📈 Upload progress: " + progress + "%");
                            runOnUiThread(() -> {
                                updateUploadProgress(progress);
                                if (progress % 20 == 0) {
                                    showInfo("Uploading... " + progress + "%");
                                }
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e(TAG, "💥 UPLOAD FAILED: " + errorMessage);

                            runOnUiThread(() -> {
                                setUploadingState(false);
                                showError("Upload failed: " + errorMessage);

                                // Detailed error dialog
                                new AlertDialog.Builder(UploadActivity.this)
                                        .setTitle("❌ Upload Failed")
                                        .setMessage("Error: " + errorMessage + "\n\nPlease try again or contact support.")
                                        .setPositiveButton("Retry", (dialog, which) -> validateAndUploadPdf())
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            });
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "💥 Exception in upload: " + e.getMessage(), e);
            setUploadingState(false);
            showError("Upload failed: " + e.getMessage());
        }
    }

    /**
     * ADD this debug button to your onCreate method:
     * Add this after your existing setupUI() call
     */
    private void addDebugButton() {
        try {
            // Your layout has a main LinearLayout inside ScrollView
            // Get the ScrollView first, then its child LinearLayout
            ScrollView scrollView = (ScrollView) binding.getRoot();
            LinearLayout mainContainer = (LinearLayout) scrollView.getChildAt(0);

            if (mainContainer != null) {
                // Create debug button
                Button debugButton = new Button(this);
                debugButton.setText("🔍 Debug Upload");
                debugButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                debugButton.setOnClickListener(v -> {
                    Log.d(TAG, "🔍 Debug button clicked");
                    debugUploadFlow();
                });

                // Add debug button to the main container
                mainContainer.addView(debugButton);

                Log.d(TAG, "✅ Debug button added successfully");
                showInfo("Debug button added - click it to test upload flow");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add debug button: " + e.getMessage());
        }

        // Fallback: Add debug functionality to existing upload button (long press)
        uploadPdfButton.setOnLongClickListener(v -> {
            Log.d(TAG, "🔍 Upload button long-clicked for debug");
            debugUploadFlow();
            return true;
        });

        Log.d(TAG, "✅ Debug functionality added to upload button (long press)");
        showInfo("Long press 'Upload PDF' button for debug mode");
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

    // User feedback methods - CORRECTED: Single clean implementation
    private void showSuccess(String message) {
        Toast.makeText(this, "✅ " + message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Success: " + message);
    }

    private void showError(String message) {
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }

    private void showInfo(String message) {
        Toast.makeText(this, "ℹ️ " + message, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Info: " + message);
    }

    // Activity lifecycle - CORRECTED: Use GoogleAuthRepository
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
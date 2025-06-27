package com.example.ddu_e_connect.presentation.view.papers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.data.repository.GoogleAuthRepository;
import com.example.ddu_e_connect.databinding.ActivityUploadBinding;
import com.example.ddu_e_connect.presentation.view.auth.SignInActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = "UploadActivity";
    private static final String STORAGE_PATH = "uploads/";

    private ActivityUploadBinding binding;
    private GoogleAuthRepository authRepository;
    private GoogleSignInAccount currentUser;
    private Uri selectedPdfUri;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupUI();
        checkUserPermissions();

        Log.d(TAG, "UploadActivity initialized");
    }

    /**
     * Initialize components
     */
    private void initializeComponents() {
        authRepository = new GoogleAuthRepository(this);
        currentUser = authRepository.getCurrentUser();
        storage = FirebaseStorage.getInstance();

        setupPdfPickerLauncher();
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
     * Setup UI components
     */
    private void setupUI() {
        setupClickListeners();
        updateUploadButtonState();
        displayUserInfo();
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Select PDF button
        binding.selectPdfButton.setOnClickListener(v -> selectPdfFile());

        // Upload PDF button
        binding.uploadPdfButton.setOnClickListener(v -> validateAndUploadPdf());
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
                checkRolePermissions(role);
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
     * Check if user role allows uploads
     */
    private void checkRolePermissions(String role) {
        boolean canUpload = "admin".equalsIgnoreCase(role) || "helper".equalsIgnoreCase(role);

        if (!canUpload) {
            Log.w(TAG, "User does not have upload permissions. Role: " + role);
            showError("You don't have permission to upload files. Contact admin for access.");
            disableUploadFeatures();
        } else {
            Log.d(TAG, "User has upload permissions");
            enableUploadFeatures();
        }
    }

    /**
     * Enable upload features
     */
    private void enableUploadFeatures() {
        binding.selectPdfButton.setEnabled(true);
        binding.pdfNameEditText.setEnabled(true);
        binding.folderNameEditText.setEnabled(true);

        // Show welcome message for authorized users
        Toast.makeText(this, "You can upload PDFs to share with students", Toast.LENGTH_LONG).show();
    }

    /**
     * Disable upload features
     */
    private void disableUploadFeatures() {
        binding.selectPdfButton.setEnabled(false);
        binding.uploadPdfButton.setEnabled(false);
        binding.pdfNameEditText.setEnabled(false);
        binding.folderNameEditText.setEnabled(false);

        // Change button text to indicate no permission
        binding.selectPdfButton.setText("Upload Not Allowed");
        binding.uploadPdfButton.setText("Permission Required");
    }

    /**
     * Display current user information
     */
    private void displayUserInfo() {
        if (currentUser != null) {
            Log.d(TAG, "Displaying info for user: " + currentUser.getDisplayName());

            // If you have user info TextViews in your layout, update them here
            // Example:
            // binding.userNameText.setText(currentUser.getDisplayName());
            // binding.userEmailText.setText(currentUser.getEmail());
        }
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
        // Update UI to show file selected
        binding.selectPdfButton.setText("PDF Selected ✓");
        binding.selectPdfButton.setEnabled(true);

        // Enable upload button
        updateUploadButtonState();

        // Show success message
        showSuccess("PDF file selected successfully!");

        Log.d(TAG, "PDF selection completed successfully");
    }

    /**
     * Validate inputs and upload PDF
     */
    private void validateAndUploadPdf() {
        String pdfName = binding.pdfNameEditText.getText().toString().trim();
        String folderName = binding.folderNameEditText.getText().toString().trim();

        Log.d(TAG, "Validating upload inputs - PDF name: " + pdfName + ", Folder: " + folderName);

        // Validate inputs
        if (selectedPdfUri == null) {
            showError("Please select a PDF file first");
            return;
        }

        if (pdfName.isEmpty()) {
            showError("Please enter a name for the PDF");
            binding.pdfNameEditText.requestFocus();
            return;
        }

        // Validate PDF name (no special characters)
        if (!isValidFileName(pdfName)) {
            showError("PDF name can only contain letters, numbers, spaces, hyphens, and underscores");
            binding.pdfNameEditText.requestFocus();
            return;
        }

        // Validate folder name if provided
        if (!folderName.isEmpty() && !isValidFileName(folderName)) {
            showError("Folder name can only contain letters, numbers, spaces, hyphens, and underscores");
            binding.folderNameEditText.requestFocus();
            return;
        }

        // Start upload
        startPdfUpload(pdfName, folderName);
    }

    /**
     * Validate file name for Firebase Storage
     */
    private boolean isValidFileName(String fileName) {
        return fileName.matches("^[a-zA-Z0-9\\s\\-_]+$");
    }

    /**
     * Start PDF upload process
     */
    private void startPdfUpload(String pdfName, String folderName) {
        Log.d(TAG, "Starting PDF upload process");

        // Set loading state
        setUploadingState(true);

        // Create storage path
        String uploadPath = createUploadPath(pdfName, folderName);
        StorageReference storageRef = storage.getReference(uploadPath);

        Log.d(TAG, "Uploading to path: " + uploadPath);

        // Start upload
        UploadTask uploadTask = storageRef.putFile(selectedPdfUri);

        uploadTask
                .addOnProgressListener(taskSnapshot -> {
                    // Update progress
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    updateUploadProgress((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "PDF upload successful");
                    onUploadSuccess(pdfName, folderName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "PDF upload failed", e);
                    onUploadFailure(e.getMessage());
                });
    }

    /**
     * Create upload path for Firebase Storage
     */
    private String createUploadPath(String pdfName, String folderName) {
        String fileName = pdfName + ".pdf";

        if (folderName != null && !folderName.trim().isEmpty()) {
            return STORAGE_PATH + folderName.trim() + "/" + fileName;
        } else {
            return STORAGE_PATH + fileName;
        }
    }

    /**
     * Update upload progress
     */
    private void updateUploadProgress(int progress) {
        runOnUiThread(() -> {
            binding.uploadPdfButton.setText("Uploading... " + progress + "%");
        });
    }

    /**
     * Handle successful upload
     */
    private void onUploadSuccess(String pdfName, String folderName) {
        setUploadingState(false);
        showSuccess("PDF uploaded successfully!");

        Log.d(TAG, "Upload completed successfully");

        // Reset form
        resetUploadForm();

        // Navigate back to home after short delay
        new android.os.Handler().postDelayed(() -> navigateToHome(), 2000);
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
     * Set uploading state for UI
     */
    private void setUploadingState(boolean isUploading) {
        binding.selectPdfButton.setEnabled(!isUploading);
        binding.uploadPdfButton.setEnabled(!isUploading);
        binding.pdfNameEditText.setEnabled(!isUploading);
        binding.folderNameEditText.setEnabled(!isUploading);

        if (!isUploading) {
            binding.uploadPdfButton.setText("Upload PDF");
        }
    }

    /**
     * Update upload button state
     */
    private void updateUploadButtonState() {
        boolean canUpload = selectedPdfUri != null &&
                !binding.pdfNameEditText.getText().toString().trim().isEmpty();
        binding.uploadPdfButton.setEnabled(canUpload);
    }

    /**
     * Reset upload form
     */
    private void resetUploadForm() {
        selectedPdfUri = null;
        binding.pdfNameEditText.setText("");
        binding.folderNameEditText.setText("");
        binding.selectPdfButton.setText("Select PDF");
        binding.uploadPdfButton.setText("Upload PDF");
        binding.uploadPdfButton.setEnabled(false);
    }

    /**
     * Navigate to home activity
     */
    private void navigateToHome() {
        Log.d(TAG, "Navigating to HomeActivity");

        try {
            Intent intent = new Intent(UploadActivity.this,
                    com.example.ddu_e_connect.presentation.view.home.HomeActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to HomeActivity", e);
        }
    }

    /**
     * Navigate to sign in activity
     */
    private void navigateToSignIn() {
        Log.d(TAG, "Navigating to SignInActivity");

        try {
            Intent intent = new Intent(UploadActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to SignInActivity", e);
        }
    }

    // Utility methods for user feedback

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showInfo(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check authentication when activity starts
        if (!authRepository.isUserSignedIn()) {
            Log.w(TAG, "User not signed in");
            navigateToSignIn();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "UploadActivity destroyed");
    }

    @Override
    public void onBackPressed() {
        // Navigate back to home
        super.onBackPressed();
        navigateToHome();
    }
}
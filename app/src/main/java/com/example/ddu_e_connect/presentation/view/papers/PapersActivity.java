package com.example.ddu_e_connect.presentation.view.papers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.adapters.PapersAdapter;
import com.example.ddu_e_connect.data.repository.GoogleAuthRepository;
import com.example.ddu_e_connect.model.FolderModel;
import com.example.ddu_e_connect.presentation.view.auth.SignInActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class PapersActivity extends AppCompatActivity {
    private static final String TAG = "PapersActivity";
    private static final String STORAGE_ROOT_PATH = "uploads/";

    private RecyclerView recyclerView;
    private PapersAdapter papersAdapter;
    private List<FolderModel> folderList = new ArrayList<>();
    private FirebaseStorage storage;
    private GoogleAuthRepository authRepository;
    private GoogleSignInAccount currentUser;

    // Navigation tracking
    private String currentPath;
    private Stack<String> navigationStack = new Stack<>();
    private Stack<String> pathTitleStack = new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_papers);

        initializeComponents();
        setupUI();
        checkUserAuthentication();
        loadInitialContent();

        Log.d(TAG, "PapersActivity initialized");
    }

    /**
     * Initialize components
     */
    private void initializeComponents() {
        recyclerView = findViewById(R.id.recycler_view);
        storage = FirebaseStorage.getInstance();
        authRepository = new GoogleAuthRepository(this);
        currentUser = authRepository.getCurrentUser();

        // Initialize navigation
        currentPath = STORAGE_ROOT_PATH;
        navigationStack.push(currentPath);
        pathTitleStack.push("Papers");
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setupRecyclerView();
        updateTitle();
    }

    /**
     * Check user authentication
     */
    private void checkUserAuthentication() {
        if (currentUser == null) {
            Log.w(TAG, "No user signed in");
            showError("You must be signed in to access papers");
            navigateToSignIn();
            return;
        }

        Log.d(TAG, "User authenticated: " + currentUser.getEmail());

        // Optionally check user role for enhanced features
        checkUserRole();
    }

    /**
     * Check user role for additional features
     */
    private void checkUserRole() {
        authRepository.fetchUserRole(currentUser.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                Log.d(TAG, "User role: " + role);
                // You can add role-specific features here
                // For example, admins might see additional options
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Could not fetch user role: " + errorMessage);
                // Continue without role-specific features
            }
        });
    }

    /**
     * Load initial content
     */
    private void loadInitialContent() {
        showLoadingState(true);
        loadFoldersAndFiles(currentPath);
    }

    /**
     * Load folders and files from Firebase Storage
     */
    private void loadFoldersAndFiles(String path) {
        Log.d(TAG, "Loading content from path: " + path);

        StorageReference listRef = storage.getReference().child(path);

        listRef.listAll()
                .addOnSuccessListener(listResult -> {
                    Log.d(TAG, "Successfully loaded content. Folders: " + listResult.getPrefixes().size() +
                            ", Files: " + listResult.getItems().size());

                    folderList.clear();

                    // Add folders to folderList
                    for (StorageReference folderRef : listResult.getPrefixes()) {
                        String folderName = folderRef.getName();
                        folderList.add(new FolderModel(folderName, false));
                        Log.d(TAG, "Added folder: " + folderName);
                    }

                    // Add PDF files to folderList
                    for (StorageReference fileRef : listResult.getItems()) {
                        String fileName = fileRef.getName();
                        if (fileName.toLowerCase().endsWith(".pdf")) {
                            folderList.add(new FolderModel(fileName, true));
                            Log.d(TAG, "Added PDF: " + fileName);
                        }
                    }

                    // Update UI
                    runOnUiThread(() -> {
                        showLoadingState(false);
                        updateRecyclerView();
                        updateEmptyState();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load content from path: " + path, e);
                    runOnUiThread(() -> {
                        showLoadingState(false);
                        showError("Failed to load papers: " + e.getMessage());
                        updateEmptyState();
                    });
                });
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        papersAdapter = new PapersAdapter(folderList, item -> {
            handleItemClick(item);
        }, this);
        recyclerView.setAdapter(papersAdapter);
    }

    /**
     * Update RecyclerView content
     */
    private void updateRecyclerView() {
        if (papersAdapter != null) {
            papersAdapter.notifyDataSetChanged();
            Log.d(TAG, "RecyclerView updated with " + folderList.size() + " items");
        }
    }

    /**
     * Handle item click (folder or PDF)
     */
    private void handleItemClick(FolderModel item) {
        if (item.isPdf()) {
            Log.d(TAG, "PDF clicked: " + item.getName());
            openPdf(item.getName());
        } else {
            Log.d(TAG, "Folder clicked: " + item.getName());
            navigateToFolder(item.getName());
        }
    }

    /**
     * Navigate to folder
     */
    private void navigateToFolder(String folderName) {
        Log.d(TAG, "Navigating to folder: " + folderName);

        // Add current state to navigation stack
        navigationStack.push(currentPath);
        pathTitleStack.push(getCurrentTitle());

        // Update current path
        currentPath += folderName + "/";

        // Update title and load content
        updateTitle();
        showLoadingState(true);
        loadFoldersAndFiles(currentPath);
    }

    /**
     * Open PDF file
     */
    private void openPdf(String fileName) {
        Log.d(TAG, "Opening PDF: " + fileName);

        showLoadingState(true);

        StorageReference pdfRef = storage.getReference().child(currentPath + fileName);

        pdfRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d(TAG, "PDF download URL obtained: " + uri.toString());
                    runOnUiThread(() -> {
                        showLoadingState(false);
                        openPdfInViewer(uri, fileName);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get PDF download URL", e);
                    runOnUiThread(() -> {
                        showLoadingState(false);
                        showError("Failed to open PDF: " + e.getMessage());
                    });
                });
    }

    /**
     * Open PDF in external viewer
     */
    private void openPdfInViewer(Uri pdfUri, String fileName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                Log.d(TAG, "PDF opened successfully: " + fileName);
            } else {
                // No PDF viewer available, offer download
                offerPdfDownload(pdfUri, fileName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF viewer", e);
            showError("Failed to open PDF. Please install a PDF viewer app.");
        }
    }

    /**
     * Offer PDF download if no viewer is available
     */
    private void offerPdfDownload(Uri pdfUri, String fileName) {
        try {
            Intent downloadIntent = new Intent(Intent.ACTION_VIEW, pdfUri);
            downloadIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(downloadIntent);

            showInfo("Opening PDF in browser for download");
        } catch (Exception e) {
            Log.e(TAG, "Failed to offer PDF download", e);
            showError("Cannot open PDF. Please install a PDF viewer app.");
        }
    }

    /**
     * Handle back navigation
     */
    private void navigateBack() {
        if (navigationStack.size() > 1) {
            // Remove current path
            navigationStack.pop();
            pathTitleStack.pop();

            // Restore previous path
            currentPath = navigationStack.peek();

            Log.d(TAG, "Navigating back to: " + currentPath);

            updateTitle();
            showLoadingState(true);
            loadFoldersAndFiles(currentPath);
        } else {
            // At root level, exit activity
            finish();
        }
    }

    /**
     * Update activity title based on current path
     */
    private void updateTitle() {
        String title = getCurrentTitle();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        Log.d(TAG, "Title updated to: " + title);
    }

    /**
     * Get current title based on navigation stack
     */
    private String getCurrentTitle() {
        if (pathTitleStack.isEmpty()) {
            return "Papers";
        }

        StringBuilder title = new StringBuilder();
        for (int i = 0; i < pathTitleStack.size(); i++) {
            if (i > 0) {
                title.append(" > ");
            }
            title.append(pathTitleStack.get(i));
        }

        return title.toString();
    }

    /**
     * Show/hide loading state
     */
    private void showLoadingState(boolean isLoading) {
        // If you have a progress bar in your layout, show/hide it here
        // For now, we'll just disable the RecyclerView during loading
        recyclerView.setEnabled(!isLoading);

        if (isLoading) {
            Log.d(TAG, "Showing loading state");
        } else {
            Log.d(TAG, "Hiding loading state");
        }
    }

    /**
     * Update empty state display
     */
    private void updateEmptyState() {
        if (folderList.isEmpty()) {
            // Show empty state message
            showInfo("No papers found in this folder");
            Log.d(TAG, "Showing empty state");
        }
    }

    /**
     * Navigate to sign in activity
     */
    private void navigateToSignIn() {
        Log.d(TAG, "Navigating to SignInActivity");

        try {
            Intent intent = new Intent(PapersActivity.this, SignInActivity.class);
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

    // Activity lifecycle and navigation

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
    public void onBackPressed() {
        // Handle custom back navigation
        super.onBackPressed();
        navigateBack();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PapersActivity destroyed");
    }
}
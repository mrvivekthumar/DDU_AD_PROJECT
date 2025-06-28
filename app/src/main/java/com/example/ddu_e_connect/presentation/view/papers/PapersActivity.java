package com.example.ddu_e_connect.presentation.view.papers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.presentation.ui.adapter.PapersAdapter;
import com.example.ddu_e_connect.data.source.remote.GoogleAuthRepository;
import com.example.ddu_e_connect.data.source.remote.GoogleDriveRepository;
import com.example.ddu_e_connect.domain.model.FolderModel;
import com.example.ddu_e_connect.presentation.view.auth.SignInActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class PapersActivity extends AppCompatActivity {
    private static final String TAG = "PapersActivity";
    private static final int DRIVE_AUTHORIZATION_REQUEST_CODE = 1001;

    // Google Drive category paths
    private static final String CATEGORY_ACADEMIC = "academic";
    private static final String CATEGORY_STUDY = "study";
    private static final String CATEGORY_EXAM = "exam";
    private static final String CATEGORY_CLUB = "club";

    private RecyclerView recyclerView;
    private PapersAdapter papersAdapter;
    private List<FolderModel> folderList = new ArrayList<>();
    private GoogleDriveRepository driveRepository;
    private GoogleAuthRepository authRepository;
    private GoogleSignInAccount currentUser;

    // Navigation tracking
    private String currentCategory;
    private String currentFolderId;
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

        Log.d(TAG, "PapersActivity initialized with Google Drive");
    }

    /**
     * Initialize components
     */
    private void initializeComponents() {
        recyclerView = findViewById(R.id.recycler_view);
        driveRepository = new GoogleDriveRepository(this);
        authRepository = new GoogleAuthRepository(this);
        currentUser = authRepository.getCurrentUser();

        // Initialize navigation with default category
        currentCategory = CATEGORY_ACADEMIC;
        currentFolderId = null;
        navigationStack.push("root");
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

        // Check user role for additional features
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

        // First request Drive permission, then load categories
        driveRepository.requestDrivePermission(new GoogleDriveRepository.AuthCallback() {
            @Override
            public void onAuthSuccess() {
                Log.d(TAG, "Drive permission granted");
                loadCategories();
            }

            @Override
            public void onAuthFailure(String errorMessage) {
                Log.e(TAG, "Drive permission failed: " + errorMessage);
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showError("Google Drive permission required: " + errorMessage);
                    updateEmptyState();
                });
            }
        });
    }

    /**
     * Load main categories (Academic, Study, Exam, Club)
     */
    private void loadCategories() {
        Log.d(TAG, "Loading main categories");

        folderList.clear();

        // Add main categories as folders
        folderList.add(new FolderModel("📚 Academic Papers", false));
        folderList.add(new FolderModel("📖 Study Materials", false));
        folderList.add(new FolderModel("📝 Exam Papers", false));
        folderList.add(new FolderModel("🏛️ Club Documents", false));

        runOnUiThread(() -> {
            showLoadingState(false);
            updateRecyclerView();
            updateEmptyState();
        });

        Log.d(TAG, "Main categories loaded");
    }

    /**
     * Load folders for specific category
     */
    private void loadCategoryFolders(String category) {
        Log.d(TAG, "Loading folders for category: " + category);

        showLoadingState(true);

        driveRepository.getFoldersInCategory(category, new GoogleDriveRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<GoogleDriveRepository.DriveFolder> folders) {
                Log.d(TAG, "Loaded " + folders.size() + " folders from category: " + category);

                folderList.clear();

                // Add folders to folderList
                for (GoogleDriveRepository.DriveFolder folder : folders) {
                    folderList.add(new FolderModel(folder.getName(), false));
                    Log.d(TAG, "Added folder: " + folder.getName());
                }

                // Update UI
                runOnUiThread(() -> {
                    showLoadingState(false);
                    updateRecyclerView();
                    updateEmptyState();
                });
            }

            @Override
            public void onFolderCreated(String folderId, String folderName) {
                // Not used in this context
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load folders for category " + category + ": " + errorMessage);
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showError("Failed to load folders: " + errorMessage);
                    updateEmptyState();
                });
            }
        });
    }

    /**
     * Load files in specific folder
     */
    private void loadFolderFiles(String folderId) {
        Log.d(TAG, "Loading files in folder: " + folderId);

        showLoadingState(true);

        driveRepository.getFilesInFolder(folderId, new GoogleDriveRepository.FilesCallback() {
            @Override
            public void onFilesLoaded(List<GoogleDriveRepository.DriveFile> files) {
                Log.d(TAG, "Loaded " + files.size() + " files from folder");

                folderList.clear();

                // Add PDF files to folderList
                for (GoogleDriveRepository.DriveFile file : files) {
                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                        folderList.add(new FolderModel(file.getName(), true));
                        Log.d(TAG, "Added PDF: " + file.getName());
                    }
                }

                // Update UI
                runOnUiThread(() -> {
                    showLoadingState(false);
                    updateRecyclerView();
                    updateEmptyState();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load files: " + errorMessage);
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showError("Failed to load files: " + errorMessage);
                    updateEmptyState();
                });
            }
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
     * Enhanced method to handle item clicks with better navigation
     */
    private void handleItemClick(FolderModel item) {
        if (item.isPdf()) {
            Log.d(TAG, "PDF clicked: " + item.getName());
            openPdf(item.getName());
        } else {
            Log.d(TAG, "Folder/Category clicked: " + item.getName());
            navigateToItem(item.getName());
        }
    }

    /**
     * Get category key from display name
     */
    private String getCategoryFromDisplayName(String displayName) {
        if (displayName.contains("Academic")) return CATEGORY_ACADEMIC;
        if (displayName.contains("Study")) return CATEGORY_STUDY;
        if (displayName.contains("Exam")) return CATEGORY_EXAM;
        if (displayName.contains("Club")) return CATEGORY_CLUB;
        return null;
    }

    /**
     * Open PDF file from Google Drive
     */
    private void openPdf(String fileName) {
        Log.d(TAG, "Opening PDF: " + fileName);

        showLoadingState(true);
        showInfo("Loading PDF: " + fileName);

        // We need to get the file's web view link from Google Drive
        // For now, we'll search for the file by name in the current folder/category
        findAndOpenPdfFile(fileName);
    }

    /**
     * Find PDF file by name and open it
     */
    private void findAndOpenPdfFile(String fileName) {
        // If we're in a specific folder, search in that folder
        if (currentFolderId != null) {
            openPdfFromFolder(currentFolderId, fileName);
        } else if (currentCategory != null) {
            // Search in the category's root
            openPdfFromCategory(currentCategory, fileName);
        } else {
            showLoadingState(false);
            showError("Cannot determine PDF location");
        }
    }

    /**
     * Open PDF from specific folder
     */
    private void openPdfFromFolder(String folderId, String fileName) {
        driveRepository.getFilesInFolder(folderId, new GoogleDriveRepository.FilesCallback() {
            @Override
            public void onFilesLoaded(List<GoogleDriveRepository.DriveFile> files) {
                // Find the specific PDF file
                GoogleDriveRepository.DriveFile foundFile = null;

                for (GoogleDriveRepository.DriveFile file : files) {
                    if (file.getName().equals(fileName)) {
                        foundFile = file;
                        break;
                    }
                }

                // Make final reference for lambda
                final GoogleDriveRepository.DriveFile finalFoundFile = foundFile;

                runOnUiThread(() -> {
                    showLoadingState(false);

                    if (finalFoundFile != null) {
                        openPdfWithWebLink(finalFoundFile.getWebViewLink(), fileName);
                    } else {
                        showError("PDF file not found: " + fileName);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showError("Failed to find PDF: " + errorMessage);
                });
            }
        });
    }

    /**
     * Open PDF from category (search all folders in category)
     */
    private void openPdfFromCategory(String category, String fileName) {
        // First get all folders in the category
        driveRepository.getFoldersInCategory(category, new GoogleDriveRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<GoogleDriveRepository.DriveFolder> folders) {
                // Search through each folder for the PDF
                searchPdfInFolders(folders, fileName, 0);
            }

            @Override
            public void onFolderCreated(String folderId, String folderName) {
                // Not used
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showError("Failed to search for PDF: " + errorMessage);
                });
            }
        });
    }

    /**
     * Recursively search for PDF in folders
     */
    private void searchPdfInFolders(List<GoogleDriveRepository.DriveFolder> folders, String fileName, int folderIndex) {
        if (folderIndex >= folders.size()) {
            // Searched all folders, PDF not found
            runOnUiThread(() -> {
                showLoadingState(false);
                showError("PDF not found in any folder: " + fileName);
            });
            return;
        }

        GoogleDriveRepository.DriveFolder currentFolder = folders.get(folderIndex);

        driveRepository.getFilesInFolder(currentFolder.getId(), new GoogleDriveRepository.FilesCallback() {
            @Override
            public void onFilesLoaded(List<GoogleDriveRepository.DriveFile> files) {
                // Check if PDF is in this folder
                GoogleDriveRepository.DriveFile targetFile = null;

                for (GoogleDriveRepository.DriveFile file : files) {
                    if (file.getName().equals(fileName)) {
                        targetFile = file;
                        break;
                    }
                }

                if (targetFile != null) {
                    // Found the PDF!
                    final GoogleDriveRepository.DriveFile foundFile = targetFile;
                    runOnUiThread(() -> {
                        showLoadingState(false);
                        openPdfWithWebLink(foundFile.getWebViewLink(), fileName);
                    });
                } else {
                    // Continue searching in next folder
                    searchPdfInFolders(folders, fileName, folderIndex + 1);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Continue searching even if one folder fails
                searchPdfInFolders(folders, fileName, folderIndex + 1);
            }
        });
    }

    /**
     * Open PDF using Google Drive web view link
     */
    private void openPdfWithWebLink(String webViewLink, String fileName) {
        if (webViewLink == null || webViewLink.trim().isEmpty()) {
            showError("PDF link not available for: " + fileName);
            return;
        }

        try {
            // Show options to user: View in Browser or Download
            showPdfOpenOptions(webViewLink, fileName);

        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF: " + fileName, e);
            showError("Failed to open PDF: " + e.getMessage());
        }
    }

    /**
     * Show PDF opening options to user
     */
    private void showPdfOpenOptions(String webViewLink, String fileName) {
        String[] options = {
                "🌐 Open in Browser",
                "📱 Open in PDF App",
                "📋 Copy Link",
                "❌ Cancel"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Open PDF: " + fileName)
                .setIcon(R.drawable.ic_pdf)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Open in Browser
                            openPdfInBrowser(webViewLink, fileName);
                            break;
                        case 1: // Open in PDF App
                            openPdfInApp(webViewLink, fileName);
                            break;
                        case 2: // Copy Link
                            copyPdfLink(webViewLink, fileName);
                            break;
                        case 3: // Cancel
                            // Do nothing
                            break;
                    }
                })
                .show();
    }

    /**
     * Open PDF in browser
     */
    private void openPdfInBrowser(String webViewLink, String fileName) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webViewLink));
            startActivity(browserIntent);

            showSuccess("Opening " + fileName + " in browser");
            Log.d(TAG, "PDF opened in browser: " + fileName);

        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF in browser", e);
            showError("No browser app found");
        }
    }

    /**
     * Open PDF in dedicated PDF app
     */
    private void openPdfInApp(String webViewLink, String fileName) {
        try {
            // Convert Google Drive view link to direct download link
            String directLink = convertToDirectDownloadLink(webViewLink);

            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(Uri.parse(directLink), "application/pdf");
            pdfIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (pdfIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(pdfIntent);
                showSuccess("Opening " + fileName + " in PDF app");
            } else {
                // Fallback to browser if no PDF app is available
                showInfo("No PDF app found, opening in browser");
                openPdfInBrowser(webViewLink, fileName);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF in app", e);
            // Fallback to browser
            openPdfInBrowser(webViewLink, fileName);
        }
    }

    /**
     * Copy PDF link to clipboard
     */
    private void copyPdfLink(String webViewLink, String fileName) {
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("PDF Link", webViewLink);
            clipboard.setPrimaryClip(clip);

            showSuccess("Link copied for: " + fileName);
            Log.d(TAG, "PDF link copied: " + fileName);

        } catch (Exception e) {
            Log.e(TAG, "Failed to copy PDF link", e);
            showError("Failed to copy link");
        }
    }

    /**
     * Convert Google Drive view link to direct download link
     */
    private String convertToDirectDownloadLink(String webViewLink) {
        if (webViewLink.contains("/file/d/")) {
            // Extract file ID from the link
            String fileId = webViewLink.split("/file/d/")[1].split("/")[0];
            return "https://drive.google.com/uc?id=" + fileId + "&export=download";
        }

        // Return original link if conversion fails
        return webViewLink;
    }

    /**
     * Enhanced navigation with folder ID tracking
     */
    private void navigateToItem(String itemName) {
        // Add current state to navigation stack
        navigationStack.push(currentCategory != null ? currentCategory : "root");
        pathTitleStack.push(getCurrentTitle());

        if (currentCategory == null) {
            // Navigating from main categories to a specific category
            String category = getCategoryFromDisplayName(itemName);
            if (category != null) {
                currentCategory = category;
                currentFolderId = null; // Reset folder ID when entering new category
                updateTitle();
                loadCategoryFolders(category);
            }
        } else {
            // Navigating into a folder within a category
            // We need to find the folder ID for this folder name
            findFolderIdAndNavigate(itemName);
        }
    }

    /**
     * Find folder ID by name and navigate to it
     */
    private void findFolderIdAndNavigate(String folderName) {
        showLoadingState(true);

        driveRepository.getFoldersInCategory(currentCategory, new GoogleDriveRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<GoogleDriveRepository.DriveFolder> folders) {
                // Find the folder with matching name
                GoogleDriveRepository.DriveFolder targetFolder = null;

                for (GoogleDriveRepository.DriveFolder folder : folders) {
                    if (folder.getName().equals(folderName)) {
                        targetFolder = folder;
                        break;
                    }
                }

                if (targetFolder != null) {
                    currentFolderId = targetFolder.getId();
                    updateTitle();
                    loadFolderFiles(currentFolderId);
                } else {
                    runOnUiThread(() -> {
                        showLoadingState(false);
                        showError("Folder not found: " + folderName);
                    });
                }
            }

            @Override
            public void onFolderCreated(String folderId, String folderName) {
                // Not used
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showError("Failed to find folder: " + errorMessage);
                });
            }
        });
    }

    /**
     * Handle back navigation
     */
    private void navigateBack() {
        if (navigationStack.size() > 1) {
            // Remove current state
            navigationStack.pop();
            pathTitleStack.pop();

            // Restore previous state
            String previousState = navigationStack.peek();

            if ("root".equals(previousState)) {
                currentCategory = null;
                currentFolderId = null;
                loadCategories();
            } else {
                currentCategory = previousState;
                loadCategoryFolders(currentCategory);
            }

            Log.d(TAG, "Navigating back to: " + previousState);
            updateTitle();
        } else {
            // At root level, exit activity
            super.onBackPressed();
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
            showInfo("No papers found in this section");
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
package com.example.ddu_e_connect.presentation.view.papers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.presentation.ui.adapter.CategoryAdapter;
import com.example.ddu_e_connect.presentation.ui.adapter.PapersAdapter;
import com.example.ddu_e_connect.data.source.remote.GoogleAuthRepository;
import com.example.ddu_e_connect.data.source.remote.FirebaseStorageRepository; // ✅ NEW: Firebase Storage
import com.example.ddu_e_connect.data.source.remote.RoleManager;
import com.example.ddu_e_connect.domain.model.FolderModel;
import com.example.ddu_e_connect.presentation.view.auth.SignInActivity;
import com.example.ddu_e_connect.presentation.view.papers.UploadActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class PapersActivity extends AppCompatActivity {
    private static final String TAG = "PapersActivity";

    // Firebase Storage category paths (same as UploadActivity)
    private static final String CATEGORY_ACADEMIC = "academic";
    private static final String CATEGORY_STUDY = "study";
    private static final String CATEGORY_EXAM = "exam";
    private static final String CATEGORY_CLUB = "club";

    // UI Components
    private RecyclerView categoriesRecyclerView;
    private RecyclerView recyclerView;
    private TextView toolbarTitle;
    private TextView breadcrumbText;
    private View breadcrumbCard;
    private LinearLayout emptyStateLayout;
    private LinearLayout loadingLayout;
    private TextView loadingText;
    private TextView emptyStateTitle;
    private TextView emptyStateMessage;
    private ImageButton backButton;
    private ImageButton searchButton;
    private FloatingActionButton fabUpload;

    // Adapters and Data
    private CategoryAdapter categoryAdapter;
    private PapersAdapter papersAdapter;
    private List<CategoryAdapter.CategoryItem> categories = new ArrayList<>();
    private List<FolderModel> folderList = new ArrayList<>();

    // ✅ UPDATED: Services - Replaced Google Drive with Firebase Storage
    private FirebaseStorageRepository storageRepository; // ✅ NEW: Firebase Storage Repository
    private GoogleAuthRepository authRepository;
    private GoogleSignInAccount currentUser;

    // Navigation tracking
    private String currentCategory;
    private String currentFolderName; // ✅ UPDATED: Use folder name instead of ID
    private Stack<String> navigationStack = new Stack<>();
    private Stack<String> pathTitleStack = new Stack<>();
    private boolean isAtRootLevel = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_papers);

        initializeComponents();
        setupUI();
        checkUserAuthentication();
        loadInitialContent();

        Log.d(TAG, "✅ Enhanced PapersActivity initialized with Firebase Storage");
    }

    /**
     * ✅ UPDATED: Initialize all components with Firebase Storage
     */
    private void initializeComponents() {
        // Initialize UI components
        initializeUIComponents();

        // ✅ UPDATED: Initialize Firebase Storage instead of Google Drive
        storageRepository = new FirebaseStorageRepository(this);
        authRepository = new GoogleAuthRepository(this);
        currentUser = authRepository.getCurrentUser();

        // Initialize navigation
        navigationStack.push("root");
        pathTitleStack.push("Study Materials");
    }

    /**
     * Initialize UI components
     */
    private void initializeUIComponents() {
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        recyclerView = findViewById(R.id.recycler_view);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        breadcrumbText = findViewById(R.id.breadcrumbText);
        breadcrumbCard = findViewById(R.id.breadcrumbCard);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        loadingLayout = findViewById(R.id.loadingLayout);
        loadingText = findViewById(R.id.loadingText);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);
        backButton = findViewById(R.id.backButton);
        searchButton = findViewById(R.id.searchButton);
        fabUpload = findViewById(R.id.fabUpload);
    }

    /**
     * Setup UI components and listeners
     */
    private void setupUI() {
        setupRecyclerViews();
        setupClickListeners();
        setupCategories();
        updateUI();
    }

    /**
     * Setup RecyclerViews
     */
    private void setupRecyclerViews() {
        // Setup categories RecyclerView with single column grid
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        categoryAdapter = new CategoryAdapter(categories, this, this::onCategorySelected);
        categoriesRecyclerView.setAdapter(categoryAdapter);

        // Setup files/folders RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        papersAdapter = new PapersAdapter(folderList, this::handleItemClick, this);
        recyclerView.setAdapter(papersAdapter);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> handleBackNavigation());

        // Search button
        searchButton.setOnClickListener(v -> showSearchDialog());

        // FAB Upload button
        fabUpload.setOnClickListener(v -> navigateToUpload());

        // Empty state back button
        findViewById(R.id.goBackButton).setOnClickListener(v -> handleBackNavigation());

        // ✅ UPDATED: Debug functionality for Firebase Storage
        searchButton.setOnLongClickListener(v -> {
            debugFirebaseStorage();
            return true;
        });

        // FAB long press for refresh
        fabUpload.setOnLongClickListener(v -> {
            refreshCurrentContent();
            return true;
        });
    }

    /**
     * Refresh current content
     */
    private void refreshCurrentContent() {
        Log.d(TAG, "Refreshing current content...");

        if (isAtRootLevel) {
            loadCategories();
        } else if (currentFolderName != null) {
            loadFolderFiles(currentCategory, currentFolderName);
        } else if (currentCategory != null) {
            loadCategoryContent(currentCategory);
        }

        showInfo("Refreshing content...");
    }

    /**
     * ✅ NEW: Debug Firebase Storage structure
     */
    private void debugFirebaseStorage() {
        Log.d(TAG, "🔍 Starting Firebase Storage debug...");

        storageRepository.testStorageConnection(new FirebaseStorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String result, String message) {
                Log.d(TAG, "✅ Firebase Storage connection successful");

                // Test folder loading for each category
                debugCategoryFolders();
            }

            @Override
            public void onProgress(int progress) {
                // Not used
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "❌ Firebase Storage connection failed: " + errorMessage);

                new androidx.appcompat.app.AlertDialog.Builder(PapersActivity.this)
                        .setTitle("❌ Storage Debug Failed")
                        .setMessage("Firebase Storage connection failed:\n" + errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    /**
     * ✅ NEW: Debug category folders in Firebase Storage
     */
    private void debugCategoryFolders() {
        StringBuilder debugInfo = new StringBuilder("🔍 FIREBASE STORAGE DEBUG:\n\n");

        String[] debugCategories = {CATEGORY_ACADEMIC, CATEGORY_STUDY, CATEGORY_EXAM, CATEGORY_CLUB};

        for (String category : debugCategories) {
            storageRepository.getFoldersInCategory(category, new FirebaseStorageRepository.FolderCallback() {
                @Override
                public void onFoldersLoaded(List<FirebaseStorageRepository.StorageFolder> folders) {
                    debugInfo.append("📁 Category: ").append(category.toUpperCase()).append("\n");
                    debugInfo.append("   Folders: ").append(folders.size()).append("\n");

                    for (FirebaseStorageRepository.StorageFolder folder : folders) {
                        debugInfo.append("   • ").append(folder.getName()).append("\n");
                    }
                    debugInfo.append("\n");

                    // Show result after last category
                    if (category.equals(CATEGORY_CLUB)) {
                        runOnUiThread(() -> {
                            new androidx.appcompat.app.AlertDialog.Builder(PapersActivity.this)
                                    .setTitle("🔍 Firebase Storage Structure")
                                    .setMessage(debugInfo.toString())
                                    .setPositiveButton("OK", null)
                                    .setNeutralButton("Upload Test PDF", (dialog, which) -> navigateToUpload())
                                    .show();
                        });
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    debugInfo.append("❌ Failed to load ").append(category).append(": ").append(errorMessage).append("\n\n");
                }
            });
        }
    }

    /**
     * Setup categories data
     */
    private void setupCategories() {
        categories.clear();

        categories.add(new CategoryAdapter.CategoryItem(
                "📚 Academic Papers",
                "Research papers and academic resources",
                CATEGORY_ACADEMIC,
                R.drawable.paper
        ));

        categories.add(new CategoryAdapter.CategoryItem(
                "📖 Study Materials",
                "Lecture notes and study guides",
                CATEGORY_STUDY,
                R.drawable.ic_folder
        ));

        categories.add(new CategoryAdapter.CategoryItem(
                "📝 Exam Papers",
                "Previous year question papers",
                CATEGORY_EXAM,
                R.drawable.ic_pdf
        ));

        categories.add(new CategoryAdapter.CategoryItem(
                "🏛️ Club Documents",
                "Club activities and documents",
                CATEGORY_CLUB,
                R.drawable.club
        ));

        Log.d(TAG, "Categories setup completed: " + categories.size() + " categories");
    }

    /**
     * Check user authentication and role
     */
    private void checkUserAuthentication() {
        if (currentUser == null) {
            Log.w(TAG, "No user signed in");
            showError("You must be signed in to access papers");
            navigateToSignIn();
            return;
        }

        Log.d(TAG, "User authenticated: " + currentUser.getEmail());
        checkUserRoleForFAB();
    }

    /**
     * Check user role to show/hide upload FAB
     */
    private void checkUserRoleForFAB() {
        authRepository.fetchUserRole(currentUser.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                Log.d(TAG, "User role: " + role);

                boolean canUpload = RoleManager.canUpload(role);
                fabUpload.setVisibility(canUpload ? View.VISIBLE : View.GONE);

                if (canUpload) {
                    Log.d(TAG, "FAB visible for role: " + role);
                } else {
                    Log.d(TAG, "FAB hidden for student role");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Could not fetch user role: " + errorMessage);
                fabUpload.setVisibility(View.GONE);
            }
        });
    }

    /**
     * ✅ UPDATED: Load initial content using Firebase Storage
     */
    private void loadInitialContent() {
        showLoadingState(true, "Loading categories...");

        // ✅ SIMPLIFIED: No permission request needed for Firebase Storage
        // Firebase Storage works automatically with Firebase Authentication
        loadCategories();
    }

    /**
     * Load main categories (Academic, Study, Exam, Club)
     */
    private void loadCategories() {
        Log.d(TAG, "Loading main categories");

        isAtRootLevel = true;
        updateUI();

        runOnUiThread(() -> {
            showLoadingState(false, null);
            categoryAdapter.notifyDataSetChanged();
        });

        Log.d(TAG, "✅ Main categories loaded and displayed");
    }

    /**
     * Handle category selection
     */
    private void onCategorySelected(CategoryAdapter.CategoryItem category) {
        Log.d(TAG, "Category selected: " + category.getTitle());

        currentCategory = category.getCategoryKey();
        isAtRootLevel = false;

        // Update navigation
        navigationStack.push(currentCategory);
        pathTitleStack.push(category.getTitle());

        updateUI();
        loadCategoryContent(currentCategory);
    }

    /**
     * ✅ UPDATED: Load category content (folders and direct files) from Firebase Storage
     */
    private void loadCategoryContent(String category) {
        Log.d(TAG, "🔍 Loading content for category: " + category);

        showLoadingState(true, "Loading " + getCurrentCategoryTitle() + "...");

        // Load both folders and direct files from Firebase Storage
        loadFoldersAndFiles(category);
    }

    /**
     * ✅ NEW: Load both folders and files from Firebase Storage
     */
    private void loadFoldersAndFiles(String category) {
        folderList.clear();

        // First, load folders
        storageRepository.getFoldersInCategory(category, new FirebaseStorageRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<FirebaseStorageRepository.StorageFolder> folders) {
                Log.d(TAG, "✅ Loaded " + folders.size() + " folders from category: " + category);

                // Add folders to folderList
                for (FirebaseStorageRepository.StorageFolder folder : folders) {
                    folderList.add(new FolderModel(folder.getName(), false)); // false = folder, not PDF
                    Log.d(TAG, "Added folder: " + folder.getName());
                }

                // Then, load direct files in category (not in subfolders)
                loadDirectFilesInCategory(category);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "❌ Failed to load folders for category " + category + ": " + errorMessage);

                // Still try to load direct files even if folder loading fails
                loadDirectFilesInCategory(category);
            }
        });
    }

    /**
     * ✅ NEW: Load files directly in category (not in subfolders)
     */
    private void loadDirectFilesInCategory(String category) {
        storageRepository.getFilesInCategory(category, new FirebaseStorageRepository.FilesCallback() {
            @Override
            public void onFilesLoaded(List<FirebaseStorageRepository.StorageFile> files) {
                Log.d(TAG, "✅ Loaded " + files.size() + " direct files from category: " + category);

                // Add PDF files to folderList
                for (FirebaseStorageRepository.StorageFile file : files) {
                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                        folderList.add(new FolderModel(file.getName(), true)); // true = PDF file
                        Log.d(TAG, "Added direct PDF: " + file.getName());
                    }
                }

                // Update UI on main thread
                runOnUiThread(() -> {
                    showLoadingState(false, null);
                    updateFoldersDisplay();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "❌ Failed to load direct files for category " + category + ": " + errorMessage);

                runOnUiThread(() -> {
                    showLoadingState(false, null);
                    updateFoldersDisplay(); // Show what we have (folders)
                });
            }
        });
    }

    /**
     * ✅ UPDATED: Load files in specific folder using Firebase Storage
     */
    private void loadFolderFiles(String category, String folderName) {
        Log.d(TAG, "🔍 Loading files in folder: " + folderName + " (category: " + category + ")");

        showLoadingState(true, "Loading documents...");

        storageRepository.getFilesInFolder(category, folderName, new FirebaseStorageRepository.FilesCallback() {
            @Override
            public void onFilesLoaded(List<FirebaseStorageRepository.StorageFile> files) {
                Log.d(TAG, "✅ Loaded " + files.size() + " files from Firebase Storage folder");

                folderList.clear();

                // Add PDF files to folderList
                for (FirebaseStorageRepository.StorageFile file : files) {
                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                        folderList.add(new FolderModel(file.getName(), true)); // true = PDF file
                        Log.d(TAG, "Added PDF: " + file.getName());
                    }
                }

                // Update UI on main thread
                runOnUiThread(() -> {
                    showLoadingState(false, null);
                    updateFoldersDisplay();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "❌ Failed to load files from Firebase Storage: " + errorMessage);
                runOnUiThread(() -> {
                    showLoadingState(false, null);
                    showError("Failed to load files: " + errorMessage);
                    showEmptyState("Loading Failed", "Failed to load files: " + errorMessage);
                });
            }
        });
    }

    /**
     * ✅ ENHANCED: Handle item clicks with Firebase Storage
     */
    private void handleItemClick(FolderModel item) {
        if (item.isPdf()) {
            Log.d(TAG, "📄 PDF clicked: " + item.getName());
            openPdf(item.getName());
        } else {
            Log.d(TAG, "📁 Folder clicked: " + item.getName());
            navigateToFolder(item.getName());
        }
    }

    /**
     * Navigate to folder
     */
    private void navigateToFolder(String folderName) {
        // Add to navigation stack
        navigationStack.push(folderName);
        pathTitleStack.push(folderName);

        currentFolderName = folderName;
        updateUI();

        // Load files in this folder
        loadFolderFiles(currentCategory, folderName);
    }

    /**
     * ✅ UPDATED: Open PDF file from Firebase Storage
     */
    private void openPdf(String fileName) {
        Log.d(TAG, "🚀 Opening PDF from Firebase Storage: " + fileName);

        showLoadingState(true, "Loading PDF: " + fileName);

        // Get download URL from Firebase Storage
        if (currentFolderName != null) {
            // PDF is in a subfolder
            openPdfFromFolder(currentCategory, currentFolderName, fileName);
        } else {
            // PDF is directly in category
            openPdfFromCategory(currentCategory, fileName);
        }
    }

    /**
     * ✅ NEW: Open PDF from Firebase Storage folder
     */
    private void openPdfFromFolder(String category, String folderName, String fileName) {
        storageRepository.getFileDownloadUrl(category, folderName, fileName,
                downloadUri -> {
                    runOnUiThread(() -> {
                        showLoadingState(false, null);
                        Log.d(TAG, "✅ Got download URL from Firebase Storage: " + downloadUri.toString());
                        openPdfWithUrl(downloadUri.toString(), fileName);
                    });
                },
                exception -> {
                    runOnUiThread(() -> {
                        showLoadingState(false, null);
                        Log.e(TAG, "❌ Failed to get download URL from Firebase Storage", exception);
                        showError("Failed to open PDF: " + exception.getMessage());
                    });
                });
    }

    /**
     * ✅ NEW: Open PDF directly from category (not in subfolder)
     */
    private void openPdfFromCategory(String category, String fileName) {
        storageRepository.getFileDownloadUrl(category, "", fileName, // Empty folder name for direct files
                downloadUri -> {
                    runOnUiThread(() -> {
                        showLoadingState(false, null);
                        Log.d(TAG, "✅ Got download URL from Firebase Storage: " + downloadUri.toString());
                        openPdfWithUrl(downloadUri.toString(), fileName);
                    });
                },
                exception -> {
                    runOnUiThread(() -> {
                        showLoadingState(false, null);
                        Log.e(TAG, "❌ Failed to get download URL from Firebase Storage", exception);
                        showError("Failed to open PDF: " + exception.getMessage());
                    });
                });
    }

    /**
     * ✅ UPDATED: Open PDF using Firebase Storage download URL
     */
    private void openPdfWithUrl(String downloadUrl, String fileName) {
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            showError("PDF download URL not available for: " + fileName);
            return;
        }

        try {
            showPdfOpenOptions(downloadUrl, fileName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF: " + fileName, e);
            showError("Failed to open PDF: " + e.getMessage());
        }
    }

    /**
     * Show PDF opening options
     */
    private void showPdfOpenOptions(String downloadUrl, String fileName) {
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
                        case 0:
                            openPdfInBrowser(downloadUrl, fileName);
                            break;
                        case 1:
                            openPdfInApp(downloadUrl, fileName);
                            break;
                        case 2:
                            copyPdfLink(downloadUrl, fileName);
                            break;
                        case 3:
                            break;
                    }
                })
                .show();
    }

    /**
     * Open PDF in browser
     */
    private void openPdfInBrowser(String downloadUrl, String fileName) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
            startActivity(browserIntent);
            showSuccess("Opening " + fileName + " in browser");
            Log.d(TAG, "✅ PDF opened in browser: " + fileName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF in browser", e);
            showError("No browser app found");
        }
    }

    /**
     * Open PDF in app
     */
    private void openPdfInApp(String downloadUrl, String fileName) {
        try {
            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(Uri.parse(downloadUrl), "application/pdf");
            pdfIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (pdfIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(pdfIntent);
                showSuccess("Opening " + fileName + " in PDF app");
            } else {
                showInfo("No PDF app found, opening in browser");
                openPdfInBrowser(downloadUrl, fileName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF in app", e);
            openPdfInBrowser(downloadUrl, fileName);
        }
    }

    /**
     * Copy PDF link
     */
    private void copyPdfLink(String downloadUrl, String fileName) {
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("PDF Link", downloadUrl);
            clipboard.setPrimaryClip(clip);
            showSuccess("Link copied for: " + fileName);
            Log.d(TAG, "✅ PDF link copied: " + fileName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy PDF link", e);
            showError("Failed to copy link");
        }
    }

    /**
     * Update UI based on current state
     */
    private void updateUI() {
        updateToolbarTitle();
        updateBreadcrumb();
        updateVisibility();
    }

    /**
     * Update toolbar title
     */
    private void updateToolbarTitle() {
        String title = getCurrentTitle();
        toolbarTitle.setText(title);
        Log.d(TAG, "Toolbar title updated: " + title);
    }

    /**
     * Update breadcrumb navigation
     */
    private void updateBreadcrumb() {
        if (isAtRootLevel) {
            breadcrumbCard.setVisibility(View.GONE);
        } else {
            breadcrumbCard.setVisibility(View.VISIBLE);

            StringBuilder breadcrumb = new StringBuilder("📁 ");
            for (int i = 0; i < pathTitleStack.size(); i++) {
                if (i > 0) breadcrumb.append(" > ");
                breadcrumb.append(pathTitleStack.get(i));
            }

            breadcrumbText.setText(breadcrumb.toString());
        }
    }

    /**
     * Update component visibility
     */
    private void updateVisibility() {
        if (isAtRootLevel) {
            categoriesRecyclerView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            categoriesRecyclerView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Update folders display
     */
    private void updateFoldersDisplay() {
        if (folderList.isEmpty()) {
            showEmptyState("No Documents", "This section doesn't have any documents yet.");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            papersAdapter.notifyDataSetChanged();
        }

        Log.d(TAG, "✅ Folders display updated: " + folderList.size() + " items");
    }

    /**
     * Show loading state
     */
    private void showLoadingState(boolean show, String message) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show && message != null && loadingText != null) {
            loadingText.setText(message);
        }

        if (show) {
            categoriesRecyclerView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    /**
     * ✅ ENHANCED: Show empty state with Firebase Storage specific messages
     */
    private void showEmptyState(String title, String message) {
        emptyStateLayout.setVisibility(View.VISIBLE);
        categoriesRecyclerView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        if (emptyStateTitle != null) emptyStateTitle.setText(title);
        if (emptyStateMessage != null) {
            String enhancedMessage = message;

            // Add helpful suggestions based on context
            if (title.contains("No Documents") && currentCategory != null) {
                enhancedMessage += "\n\n💡 Suggestions:";
                enhancedMessage += "\n• Upload documents using the + button";
                enhancedMessage += "\n• Check Firebase Storage in the Console";
                enhancedMessage += "\n• Long press the search button to debug Firebase Storage";
                enhancedMessage += "\n• Ensure files are uploaded to the correct category";
            }

            emptyStateMessage.setText(enhancedMessage);
        }
    }

    /**
     * Handle back navigation
     */
    private void handleBackNavigation() {
        if (navigationStack.size() > 1) {
            navigationStack.pop();
            pathTitleStack.pop();

            String previousState = navigationStack.peek();

            if ("root".equals(previousState)) {
                isAtRootLevel = true;
                currentCategory = null;
                currentFolderName = null;
                loadCategories();
            } else if (currentFolderName != null) {
                // Going back from folder to category
                currentFolderName = null;
                loadCategoryContent(currentCategory);
            } else {
                // Going back from category to category list
                currentCategory = previousState;
                loadCategoryContent(currentCategory);
            }

            updateUI();
            Log.d(TAG, "Navigated back to: " + previousState);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Show search dialog
     */
    private void showSearchDialog() {
        showInfo("Search functionality will be implemented soon");
    }

    /**
     * Navigate to upload activity
     */
    private void navigateToUpload() {
        Intent intent = new Intent(this, UploadActivity.class);
        startActivity(intent);
    }

    /**
     * Get current title for toolbar
     */
    private String getCurrentTitle() {
        if (isAtRootLevel) {
            return "📚 Study Materials";
        } else if (pathTitleStack.size() > 0) {
            return pathTitleStack.peek();
        } else {
            return "Documents";
        }
    }

    /**
     * Get current category title
     */
    private String getCurrentCategoryTitle() {
        if (currentCategory != null) {
            for (CategoryAdapter.CategoryItem cat : categories) {
                if (cat.getCategoryKey().equals(currentCategory)) {
                    return cat.getTitle();
                }
            }
        }
        return "Documents";
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

    // Activity lifecycle and navigation
    @Override
    protected void onStart() {
        super.onStart();
        if (!authRepository.isUserSignedIn()) {
            Log.w(TAG, "User not signed in");
            navigateToSignIn();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleBackNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "✅ Enhanced PapersActivity destroyed - Firebase Storage migration complete!");
    }
}
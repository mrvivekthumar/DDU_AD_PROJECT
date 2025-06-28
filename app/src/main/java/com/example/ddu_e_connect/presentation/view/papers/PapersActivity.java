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
import com.example.ddu_e_connect.data.source.remote.GoogleDriveRepository;
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

    // Google Drive category paths
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

    // Services
    private GoogleDriveRepository driveRepository;
    private GoogleAuthRepository authRepository;
    private GoogleSignInAccount currentUser;

    // Navigation tracking
    private String currentCategory;
    private String currentFolderId;
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

        Log.d(TAG, "Enhanced PapersActivity initialized with Google Drive");
    }

    /**
     * Initialize all components
     */
    private void initializeComponents() {
        // Initialize UI components
        initializeUIComponents();

        // Initialize services
        driveRepository = new GoogleDriveRepository(this);
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

        // TEMPORARY: Add debug functionality (long press on search button)
        searchButton.setOnClickListener(v -> showSearchDialog());
        searchButton.setOnLongClickListener(v -> {
            debugFolderStructure();
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
        } else if (currentFolderId != null) {
            loadFolderFiles(currentFolderId);
        } else if (currentCategory != null) {
            loadCategoryFolders(currentCategory);
        }

        showInfo("Refreshing content...");
    }

    /**
     * DEBUG: Add this method to test folder structure
     */
    private void debugFolderStructure() {
        Log.d(TAG, "Starting folder structure debug...");

        driveRepository.debugFolderStructure(new GoogleDriveRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<GoogleDriveRepository.DriveFolder> folders) {
                StringBuilder debugInfo = new StringBuilder("🔍 FOLDER STRUCTURE DEBUG:\n\n");

                debugInfo.append("Total folders found: ").append(folders.size()).append("\n\n");

                for (GoogleDriveRepository.DriveFolder folder : folders) {
                    debugInfo.append("📁 ").append(folder.getName()).append("\n");
                    debugInfo.append("   ID: ").append(folder.getId().substring(0, 8)).append("...\n\n");
                }

                if (folders.isEmpty()) {
                    debugInfo.append("❌ No folders found!\n");
                    debugInfo.append("This might explain why PDFs aren't showing.\n\n");
                    debugInfo.append("Try uploading a PDF first to create the folder structure.");
                }

                Log.d(TAG, debugInfo.toString());

                new androidx.appcompat.app.AlertDialog.Builder(PapersActivity.this)
                        .setTitle("🔍 Folder Structure Debug")
                        .setMessage(debugInfo.toString())
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Upload Test PDF", (dialog, which) -> {
                            navigateToUpload();
                        })
                        .show();
            }

            @Override
            public void onFolderCreated(String folderId, String folderName) {
                // Not used
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Folder structure debug failed: " + errorMessage);

                new androidx.appcompat.app.AlertDialog.Builder(PapersActivity.this)
                        .setTitle("❌ Debug Failed")
                        .setMessage("Failed to debug folder structure:\n" + errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
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

        // Check user role for FAB visibility
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
     * Load initial content
     */
    private void loadInitialContent() {
        showLoadingState(true, "Loading categories...");

        // Request Drive permission first
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
                    showLoadingState(false, null);
                    showError("Google Drive permission required: " + errorMessage);
                    showEmptyState("Permission Required", errorMessage);
                });
            }
        });
    }

    /**
     * Load main categories (Academic, Study, Exam, Club)
     */
    private void loadCategories() {
        Log.d(TAG, "Loading main categories");

        // Show categories at root level
        isAtRootLevel = true;
        updateUI();

        runOnUiThread(() -> {
            showLoadingState(false, null);
            categoryAdapter.notifyDataSetChanged();
        });

        Log.d(TAG, "Main categories loaded and displayed");
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
        loadCategoryFolders(currentCategory);
    }

    /**
     * Load folders for specific category
     */
    private void loadCategoryFolders(String category) {
        Log.d(TAG, "Loading folders for category: " + category);

        showLoadingState(true, "Loading " + getCurrentCategoryTitle() + "...");

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

                // Update UI on main thread
                runOnUiThread(() -> {
                    showLoadingState(false, null);
                    updateFoldersDisplay();
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
                    showLoadingState(false, null);
                    showError("Failed to load folders: " + errorMessage);
                    showEmptyState("Loading Failed", "Failed to load folders: " + errorMessage);
                });
            }
        });
    }

    /**
     * Load files in specific folder
     */
    private void loadFolderFiles(String folderId) {
        Log.d(TAG, "Loading files in folder: " + folderId);

        showLoadingState(true, "Loading documents...");

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

                // Update UI on main thread
                runOnUiThread(() -> {
                    showLoadingState(false, null);
                    updateFoldersDisplay();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load files: " + errorMessage);
                runOnUiThread(() -> {
                    showLoadingState(false, null);
                    showError("Failed to load files: " + errorMessage);
                    showEmptyState("Loading Failed", "Failed to load files: " + errorMessage);
                });
            }
        });
    }

    /**
     * Enhanced method to handle item clicks with better navigation
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
        // Add to navigation stack
        navigationStack.push(folderName);
        pathTitleStack.push(folderName);

        updateUI();
        findFolderIdAndNavigate(folderName);
    }

    /**
     * Find folder ID by name and navigate to it
     */
    private void findFolderIdAndNavigate(String folderName) {
        showLoadingState(true, "Opening folder...");

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
                    loadFolderFiles(currentFolderId);
                } else {
                    runOnUiThread(() -> {
                        showLoadingState(false, null);
                        showError("Folder not found: " + folderName);
                        showEmptyState("Folder Not Found", "The folder '" + folderName + "' could not be found.");
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
                    showLoadingState(false, null);
                    showError("Failed to find folder: " + errorMessage);
                    showEmptyState("Error", "Failed to find folder: " + errorMessage);
                });
            }
        });
    }

    /**
     * Open PDF file from Google Drive (your existing implementation)
     */
    private void openPdf(String fileName) {
        Log.d(TAG, "Opening PDF: " + fileName);

        showLoadingState(true, "Loading PDF: " + fileName);

        // Use your existing PDF opening logic
        findAndOpenPdfFile(fileName);
    }

    /**
     * Find PDF file by name and open it (your existing implementation)
     */
    private void findAndOpenPdfFile(String fileName) {
        if (currentFolderId != null) {
            openPdfFromFolder(currentFolderId, fileName);
        } else if (currentCategory != null) {
            openPdfFromCategory(currentCategory, fileName);
        } else {
            showLoadingState(false, null);
            showError("Cannot determine PDF location");
        }
    }

    /**
     * Open PDF from specific folder (your existing implementation)
     */
    private void openPdfFromFolder(String folderId, String fileName) {
        driveRepository.getFilesInFolder(folderId, new GoogleDriveRepository.FilesCallback() {
            @Override
            public void onFilesLoaded(List<GoogleDriveRepository.DriveFile> files) {
                GoogleDriveRepository.DriveFile foundFile = null;

                for (GoogleDriveRepository.DriveFile file : files) {
                    if (file.getName().equals(fileName)) {
                        foundFile = file;
                        break;
                    }
                }

                final GoogleDriveRepository.DriveFile finalFoundFile = foundFile;

                runOnUiThread(() -> {
                    showLoadingState(false, null);

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
                    showLoadingState(false, null);
                    showError("Failed to find PDF: " + errorMessage);
                });
            }
        });
    }

    /**
     * Open PDF from category (your existing implementation)
     */
    private void openPdfFromCategory(String category, String fileName) {
        driveRepository.getFoldersInCategory(category, new GoogleDriveRepository.FolderCallback() {
            @Override
            public void onFoldersLoaded(List<GoogleDriveRepository.DriveFolder> folders) {
                searchPdfInFolders(folders, fileName, 0);
            }

            @Override
            public void onFolderCreated(String folderId, String folderName) {
                // Not used
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    showLoadingState(false, null);
                    showError("Failed to search for PDF: " + errorMessage);
                });
            }
        });
    }

    /**
     * Recursively search for PDF in folders (your existing implementation)
     */
    private void searchPdfInFolders(List<GoogleDriveRepository.DriveFolder> folders, String fileName, int folderIndex) {
        if (folderIndex >= folders.size()) {
            runOnUiThread(() -> {
                showLoadingState(false, null);
                showError("PDF not found in any folder: " + fileName);
            });
            return;
        }

        GoogleDriveRepository.DriveFolder currentFolder = folders.get(folderIndex);

        driveRepository.getFilesInFolder(currentFolder.getId(), new GoogleDriveRepository.FilesCallback() {
            @Override
            public void onFilesLoaded(List<GoogleDriveRepository.DriveFile> files) {
                GoogleDriveRepository.DriveFile targetFile = null;

                for (GoogleDriveRepository.DriveFile file : files) {
                    if (file.getName().equals(fileName)) {
                        targetFile = file;
                        break;
                    }
                }

                if (targetFile != null) {
                    final GoogleDriveRepository.DriveFile foundFile = targetFile;
                    runOnUiThread(() -> {
                        showLoadingState(false, null);
                        openPdfWithWebLink(foundFile.getWebViewLink(), fileName);
                    });
                } else {
                    searchPdfInFolders(folders, fileName, folderIndex + 1);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                searchPdfInFolders(folders, fileName, folderIndex + 1);
            }
        });
    }

    /**
     * Open PDF using Google Drive web view link (your existing implementation)
     */
    private void openPdfWithWebLink(String webViewLink, String fileName) {
        if (webViewLink == null || webViewLink.trim().isEmpty()) {
            showError("PDF link not available for: " + fileName);
            return;
        }

        try {
            showPdfOpenOptions(webViewLink, fileName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF: " + fileName, e);
            showError("Failed to open PDF: " + e.getMessage());
        }
    }

    /**
     * Show PDF opening options (your existing implementation)
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
                        case 0:
                            openPdfInBrowser(webViewLink, fileName);
                            break;
                        case 1:
                            openPdfInApp(webViewLink, fileName);
                            break;
                        case 2:
                            copyPdfLink(webViewLink, fileName);
                            break;
                        case 3:
                            break;
                    }
                })
                .show();
    }

    /**
     * Open PDF in browser (your existing implementation)
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
     * Open PDF in app (your existing implementation)
     */
    private void openPdfInApp(String webViewLink, String fileName) {
        try {
            String directLink = convertToDirectDownloadLink(webViewLink);
            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(Uri.parse(directLink), "application/pdf");
            pdfIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (pdfIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(pdfIntent);
                showSuccess("Opening " + fileName + " in PDF app");
            } else {
                showInfo("No PDF app found, opening in browser");
                openPdfInBrowser(webViewLink, fileName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open PDF in app", e);
            openPdfInBrowser(webViewLink, fileName);
        }
    }

    /**
     * Copy PDF link (your existing implementation)
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
     * Convert Google Drive view link to direct download link (your existing implementation)
     */
    private String convertToDirectDownloadLink(String webViewLink) {
        if (webViewLink.contains("/file/d/")) {
            String fileId = webViewLink.split("/file/d/")[1].split("/")[0];
            return "https://drive.google.com/uc?id=" + fileId + "&export=download";
        }
        return webViewLink;
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

        Log.d(TAG, "Folders display updated: " + folderList.size() + " items");
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
     * ENHANCED: Show empty state with actionable messages
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
                enhancedMessage += "\n• Check if documents were uploaded to the correct category";
                enhancedMessage += "\n• Long press the search button to debug folder structure";
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
                currentFolderId = null;
                loadCategories();
            } else {
                currentCategory = previousState;
                currentFolderId = null;
                loadCategoryFolders(currentCategory);
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
        Log.d(TAG, "Enhanced PapersActivity destroyed");
    }
}
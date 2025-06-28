package com.example.ddu_e_connect.data.source.remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Google Drive API Repository for handling PDF uploads and folder management
 * Provides free alternative to Firebase Storage
 */
public class GoogleDriveRepository {
    private static final String TAG = "GoogleDriveRepository";
    private static final String APP_NAME = "DDU E-Connect";
    private static final int DRIVE_AUTHORIZATION_REQUEST_CODE = 1001;

    // Main folder structure for DDU E-Connect
    private static final String ROOT_FOLDER_NAME = "DDU_E_Connect_Papers";
    private static final String ACADEMIC_PAPERS_FOLDER = "Academic_Papers";
    private static final String STUDY_MATERIALS_FOLDER = "Study_Materials";
    private static final String EXAM_PAPERS_FOLDER = "Exam_Papers";
    private static final String CLUB_DOCUMENTS_FOLDER = "Club_Documents";

    private Drive driveService;
    private Context context;
    private Executor executor;
    private String rootFolderId;

    // Callback interfaces
    public interface UploadCallback {
        void onSuccess(String fileId, String fileName);
        void onProgress(int progress);
        void onFailure(String errorMessage);
    }

    public interface FolderCallback {
        void onFoldersLoaded(List<DriveFolder> folders);
        void onFolderCreated(String folderId, String folderName);
        void onFailure(String errorMessage);
    }

    public interface FilesCallback {
        void onFilesLoaded(List<DriveFile> files);
        void onFailure(String errorMessage);
    }

    public interface AuthCallback {
        void onAuthSuccess();
        void onAuthFailure(String errorMessage);
    }

    public GoogleDriveRepository(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        initializeDriveService();
    }

    /**
     * Initialize Google Drive service with proper permission handling
     */
    private void initializeDriveService() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            Log.e(TAG, "No Google account found");
            return;
        }

        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context, Collections.singleton(DriveScopes.DRIVE_FILE)
            );
            credential.setSelectedAccount(account.getAccount());

            driveService = new Drive.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName(APP_NAME)
                    .build();

            Log.d(TAG, "Google Drive service initialized successfully");

            // Test the service immediately to catch auth issues early
            testDriveServiceConnection();

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Google Drive service", e);
            driveService = null;
        }
    }

    /**
     * Test Drive service connection
     */
    private void testDriveServiceConnection() {
        executor.execute(() -> {
            try {
                // Simple test - try to list files (limited to 1)
                driveService.files().list()
                        .setPageSize(1)
                        .setFields("files(id, name)")
                        .execute();

                Log.d(TAG, "Drive service connection test: SUCCESS");

                // Initialize folder structure only after successful connection
                initializeFolderStructureAsync();

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "Drive service needs user consent: " + e.getMessage());
                handleUserRecoverableAuthError(e);
            } catch (Exception e) {
                Log.e(TAG, "Drive service connection test failed", e);
            }
        });
    }

    /**
     * Handle user recoverable auth errors
     */
    private void handleUserRecoverableAuthError(UserRecoverableAuthIOException e) {
        Log.d(TAG, "Handling user recoverable auth error");

        // We need to handle this in the UI thread through the calling activity
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    // Start the authorization intent
                    Intent intent = e.getIntent();
                    ((Activity) context).startActivityForResult(intent, DRIVE_AUTHORIZATION_REQUEST_CODE);
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to start authorization intent", ex);
                }
            });
        }
    }

    /**
     * Initialize folder structure asynchronously
     */
    private void initializeFolderStructureAsync() {
        try {
            // Check if root folder exists, create if not
            rootFolderId = findOrCreateFolder(ROOT_FOLDER_NAME, null);
            Log.d(TAG, "Root folder ID: " + rootFolderId);

            // Create main category folders
            findOrCreateFolder(ACADEMIC_PAPERS_FOLDER, rootFolderId);
            findOrCreateFolder(STUDY_MATERIALS_FOLDER, rootFolderId);
            findOrCreateFolder(EXAM_PAPERS_FOLDER, rootFolderId);
            findOrCreateFolder(CLUB_DOCUMENTS_FOLDER, rootFolderId);

            Log.d(TAG, "Folder structure initialized successfully");

        } catch (UserRecoverableAuthIOException e) {
            Log.w(TAG, "User consent needed for folder initialization");
            handleUserRecoverableAuthError(e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize folder structure", e);
        }
    }

    /**
     * Request Drive permission and initialize service
     */
    public void requestDrivePermission(AuthCallback callback) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);

        if (account == null) {
            Log.e(TAG, "No Google account found");
            callback.onAuthFailure("User not signed in");
            return;
        }

        // Re-initialize drive service if needed
        if (driveService == null) {
            Log.d(TAG, "Initializing Drive service for permission request");
            initializeDriveService();
        }

        // Test the service
        executor.execute(() -> {
            try {
                if (driveService == null) {
                    ((Activity) context).runOnUiThread(() ->
                            callback.onAuthFailure("Failed to initialize Google Drive service"));
                    return;
                }

                // Test with a simple call
                driveService.files().list()
                        .setPageSize(1)
                        .setFields("files(id)")
                        .execute();

                Log.d(TAG, "Drive permission test successful");
                ((Activity) context).runOnUiThread(() -> callback.onAuthSuccess());

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "User consent required");

                ((Activity) context).runOnUiThread(() -> {
                    // Show user-friendly dialog explaining the need for Drive permission
                    showDrivePermissionDialog(e, callback);
                });

            } catch (Exception e) {
                Log.e(TAG, "Drive permission test failed", e);
                ((Activity) context).runOnUiThread(() ->
                        callback.onAuthFailure("Drive access failed: " + e.getMessage()));
            }
        });
    }

    /**
     * Show Drive permission dialog to user
     */
    private void showDrivePermissionDialog(UserRecoverableAuthIOException authException, AuthCallback callback) {
        if (!(context instanceof Activity)) {
            callback.onAuthFailure("Cannot request permission in this context");
            return;
        }

        Activity activity = (Activity) context;

        String message = "🔐 Google Drive Permission Required\n\n" +
                "DDU E-Connect needs access to Google Drive to:\n" +
                "• 📤 Upload PDF files\n" +
                "• 📁 Create study folders\n" +
                "• 📚 Organize academic materials\n\n" +
                "This permission is safe and only allows the app to manage files it creates.";

        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("📂 Drive Access Needed")
                .setMessage(message)
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    try {
                        // Start the authorization flow
                        Intent intent = authException.getIntent();
                        activity.startActivityForResult(intent, DRIVE_AUTHORIZATION_REQUEST_CODE);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start Drive authorization", e);
                        callback.onAuthFailure("Failed to start authorization: " + e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    callback.onAuthFailure("User denied Drive permission");
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Upload PDF file to Google Drive
     */
    public void uploadPDF(java.io.File pdfFile, String fileName, String folderName,
                          String category, UploadCallback callback) {

        if (driveService == null) {
            callback.onFailure("Drive service not initialized");
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting PDF upload: " + fileName);

                // Determine target folder
                String targetFolderId = getTargetFolderId(category, folderName);

                // Create file metadata
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList(targetFolderId));

                // Create file content
                FileContent mediaContent = new FileContent("application/pdf", pdfFile);

                // Upload file
                File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id, name, size, createdTime")
                        .execute();

                Log.d(TAG, "PDF uploaded successfully: " + uploadedFile.getId());
                callback.onSuccess(uploadedFile.getId(), fileName);

            } catch (Exception e) {
                Log.e(TAG, "Failed to upload PDF", e);
                callback.onFailure("Upload failed: " + e.getMessage());
            }
        });
    }

    /**
     * Get list of available folders in a category
     */
    public void getFoldersInCategory(String category, FolderCallback callback) {
        if (driveService == null) {
            callback.onFailure("Drive service not initialized");
            return;
        }

        executor.execute(() -> {
            try {
                String categoryFolderId = getCategoryFolderId(category);

                String query = "'" + categoryFolderId + "' in parents and " +
                        "mimeType='application/vnd.google-apps.folder' and " +
                        "trashed=false";

                FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id, name, createdTime)")
                        .execute();

                List<DriveFolder> folders = new ArrayList<>();
                for (File file : result.getFiles()) {
                    folders.add(new DriveFolder(file.getId(), file.getName()));
                }

                Log.d(TAG, "Found " + folders.size() + " folders in category: " + category);
                callback.onFoldersLoaded(folders);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get folders", e);
                callback.onFailure("Failed to load folders: " + e.getMessage());
            }
        });
    }

    /**
     * Create new folder in specified category
     */
    public void createFolder(String folderName, String category, FolderCallback callback) {
        if (driveService == null) {
            callback.onFailure("Drive service not initialized");
            return;
        }

        executor.execute(() -> {
            try {
                String categoryFolderId = getCategoryFolderId(category);
                String newFolderId = findOrCreateFolder(folderName, categoryFolderId);

                Log.d(TAG, "Folder created: " + folderName + " with ID: " + newFolderId);
                callback.onFolderCreated(newFolderId, folderName);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create folder", e);
                callback.onFailure("Failed to create folder: " + e.getMessage());
            }
        });
    }

    /**
     * Get files in a specific folder
     */
    public void getFilesInFolder(String folderId, FilesCallback callback) {
        if (driveService == null) {
            callback.onFailure("Drive service not initialized");
            return;
        }

        executor.execute(() -> {
            try {
                String query = "'" + folderId + "' in parents and " +
                        "mimeType='application/pdf' and " +
                        "trashed=false";

                FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id, name, size, createdTime, webViewLink)")
                        .execute();

                List<DriveFile> files = new ArrayList<>();
                for (File file : result.getFiles()) {
                    files.add(new DriveFile(
                            file.getId(),
                            file.getName(),
                            file.getWebViewLink(),
                            file.getSize(),
                            file.getCreatedTime()
                    ));
                }

                Log.d(TAG, "Found " + files.size() + " files in folder");
                callback.onFilesLoaded(files);

            } catch (Exception e) {
                Log.e(TAG, "Failed to get files", e);
                callback.onFailure("Failed to load files: " + e.getMessage());
            }
        });
    }

    /**
     * Helper method to find or create folder
     */
    private String findOrCreateFolder(String folderName, String parentId) throws IOException {
        // Search for existing folder
        String query = "mimeType='application/vnd.google-apps.folder' and " +
                "name='" + folderName + "' and " +
                "trashed=false";

        if (parentId != null) {
            query += " and '" + parentId + "' in parents";
        }

        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        // Create new folder
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        if (parentId != null) {
            folderMetadata.setParents(Collections.singletonList(parentId));
        }

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }

    /**
     * Get category folder ID
     */
    private String getCategoryFolderId(String category) throws IOException {
        String folderName;
        switch (category.toLowerCase()) {
            case "academic":
                folderName = ACADEMIC_PAPERS_FOLDER;
                break;
            case "study":
                folderName = STUDY_MATERIALS_FOLDER;
                break;
            case "exam":
                folderName = EXAM_PAPERS_FOLDER;
                break;
            case "club":
                folderName = CLUB_DOCUMENTS_FOLDER;
                break;
            default:
                folderName = ACADEMIC_PAPERS_FOLDER;
        }

        return findOrCreateFolder(folderName, rootFolderId);
    }

    /**
     * Get target folder ID for upload
     */
    private String getTargetFolderId(String category, String folderName) throws IOException {
        String categoryFolderId = getCategoryFolderId(category);

        if (folderName != null && !folderName.trim().isEmpty()) {
            return findOrCreateFolder(folderName.trim(), categoryFolderId);
        }

        return categoryFolderId;
    }

    /**
     * Test method to verify Google Drive API setup
     */
    public void testDriveConnection(UploadCallback callback) {
        if (driveService == null) {
            callback.onFailure("Drive service not initialized");
            return;
        }

        executor.execute(() -> {
            try {
                // Simple test - list files to verify connection
                FileList result = driveService.files().list()
                        .setPageSize(1)
                        .setFields("files(id, name)")
                        .execute();

                Log.d(TAG, "Drive API connection successful! Found " + result.getFiles().size() + " file(s)");
                callback.onSuccess("connection_test", "Drive API Connected Successfully");

            } catch (Exception e) {
                Log.e(TAG, "Drive API connection failed", e);
                callback.onFailure("Connection failed: " + e.getMessage());
            }
        });
    }

    // Data classes for Drive items
    public static class DriveFolder {
        private String id;
        private String name;

        public DriveFolder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }
    }

    public static class DriveFile {
        private String id;
        private String name;
        private String webViewLink;
        private Long size;
        private com.google.api.client.util.DateTime createdTime;

        public DriveFile(String id, String name, String webViewLink, Long size,
                         com.google.api.client.util.DateTime createdTime) {
            this.id = id;
            this.name = name;
            this.webViewLink = webViewLink;
            this.size = size;
            this.createdTime = createdTime;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getWebViewLink() { return webViewLink; }
        public Long getSize() { return size; }
        public com.google.api.client.util.DateTime getCreatedTime() { return createdTime; }
    }
}
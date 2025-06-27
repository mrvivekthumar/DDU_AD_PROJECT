package com.example.ddu_e_connect.data.source.remote;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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

    // Add the missing AuthCallback interface
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
     * Initialize Google Drive service
     */
    private void initializeDriveService() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            Log.e(TAG, "No Google account found");
            return;
        }

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

        Log.d(TAG, "Google Drive service initialized");

        // Initialize folder structure
        initializeFolderStructure();
    }

    /**
     * Initialize the main folder structure for DDU E-Connect
     */
    private void initializeFolderStructure() {
        executor.execute(() -> {
            try {
                // Check if root folder exists, create if not
                rootFolderId = findOrCreateFolder(ROOT_FOLDER_NAME, null);

                // Create main category folders
                findOrCreateFolder(ACADEMIC_PAPERS_FOLDER, rootFolderId);
                findOrCreateFolder(STUDY_MATERIALS_FOLDER, rootFolderId);
                findOrCreateFolder(EXAM_PAPERS_FOLDER, rootFolderId);
                findOrCreateFolder(CLUB_DOCUMENTS_FOLDER, rootFolderId);

                Log.d(TAG, "Folder structure initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize folder structure", e);
            }
        });
    }

    /**
     * Request Drive permission and initialize service
     * This method was missing and causing the error
     */
    public void requestDrivePermission(AuthCallback callback) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);

        if (account == null) {
            Log.e(TAG, "No Google account found");
            callback.onAuthFailure("User not signed in");
            return;
        }

        // Check if we already have permission
        if (driveService != null) {
            Log.d(TAG, "Drive service already initialized");
            callback.onAuthSuccess();
            return;
        }

        try {
            // Re-initialize drive service
            initializeDriveService();

            if (driveService != null) {
                Log.d(TAG, "Drive permission granted successfully");
                callback.onAuthSuccess();
            } else {
                Log.e(TAG, "Failed to initialize Drive service");
                callback.onAuthFailure("Failed to initialize Google Drive service");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting Drive permission", e);
            callback.onAuthFailure("Error: " + e.getMessage());
        }
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
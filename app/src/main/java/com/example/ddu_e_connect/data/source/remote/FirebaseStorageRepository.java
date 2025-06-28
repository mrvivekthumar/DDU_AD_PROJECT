package com.example.ddu_e_connect.data.source.remote;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Storage Repository for handling PDF uploads and folder management
 * Replaces Google Drive API with Firebase Storage
 */
public class FirebaseStorageRepository {
    private static final String TAG = "FirebaseStorageRepo";

    // Storage structure for DDU E-Connect
    private static final String ROOT_FOLDER = "ddu_papers";
    private static final String ACADEMIC_FOLDER = "academic";
    private static final String STUDY_FOLDER = "study";
    private static final String EXAM_FOLDER = "exam";
    private static final String CLUB_FOLDER = "club";

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Context context;

    // Callback interfaces
    public interface UploadCallback {
        void onSuccess(String downloadUrl, String fileName);
        void onProgress(int progress);
        void onFailure(String errorMessage);
    }

    public interface FolderCallback {
        void onFoldersLoaded(List<StorageFolder> folders);
        void onFailure(String errorMessage);
    }

    public interface FilesCallback {
        void onFilesLoaded(List<StorageFile> files);
        void onFailure(String errorMessage);
    }

    public FirebaseStorageRepository(Context context) {
        this.context = context;
        this.storage = FirebaseStorage.getInstance();
        this.storageRef = storage.getReference();

        Log.d(TAG, "Firebase Storage Repository initialized");
    }

    /**
     * Upload PDF file to Firebase Storage
     */
    public void uploadPDF(File pdfFile, String fileName, String folderName,
                          String category, UploadCallback callback) {

        if (pdfFile == null || !pdfFile.exists()) {
            callback.onFailure("PDF file not found");
            return;
        }

        Log.d(TAG, "Starting PDF upload: " + fileName + " to category: " + category);

        try {
            // Create the storage path
            String storagePath = buildStoragePath(category, folderName, fileName);
            StorageReference fileRef = storageRef.child(storagePath);

            // Create metadata for the file
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("application/pdf")
                    .setCustomMetadata("category", category)
                    .setCustomMetadata("folder", folderName != null ? folderName : "")
                    .setCustomMetadata("uploadedBy", "DDU_E_Connect")
                    .setCustomMetadata("uploadTime", String.valueOf(System.currentTimeMillis()))
                    .build();

            // Convert File to Uri
            Uri fileUri = Uri.fromFile(pdfFile);

            // Start upload
            UploadTask uploadTask = fileRef.putFile(fileUri, metadata);

            // Monitor progress
            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    callback.onProgress((int) progress);
                    Log.d(TAG, "Upload progress: " + (int) progress + "%");
                }
            });

            // Handle success
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Get download URL
                    fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUri) {
                            Log.d(TAG, "PDF uploaded successfully: " + downloadUri.toString());
                            callback.onSuccess(downloadUri.toString(), fileName);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Failed to get download URL", e);
                            callback.onFailure("Upload completed but failed to get download URL: " + e.getMessage());
                        }
                    });
                }
            });

            // Handle failure
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "PDF upload failed", e);
                    callback.onFailure("Upload failed: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to start upload", e);
            callback.onFailure("Failed to start upload: " + e.getMessage());
        }
    }

    /**
     * Get list of folders in a category
     */
    public void getFoldersInCategory(String category, FolderCallback callback) {
        Log.d(TAG, "Getting folders for category: " + category);

        String categoryPath = ROOT_FOLDER + "/" + category + "/";
        StorageReference categoryRef = storageRef.child(categoryPath);

        categoryRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                List<StorageFolder> folders = new ArrayList<>();

                // Get folder prefixes (these represent subdirectories)
                for (StorageReference prefix : listResult.getPrefixes()) {
                    String folderName = prefix.getName();
                    folders.add(new StorageFolder(folderName, prefix.getPath()));
                    Log.d(TAG, "Found folder: " + folderName);
                }

                Log.d(TAG, "Loaded " + folders.size() + " folders for category: " + category);
                callback.onFoldersLoaded(folders);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to get folders for category: " + category, e);
                callback.onFailure("Failed to load folders: " + e.getMessage());
            }
        });
    }

    /**
     * Get files in a specific folder
     */
    public void getFilesInFolder(String category, String folderName, FilesCallback callback) {
        Log.d(TAG, "Getting files in folder: " + folderName + " category: " + category);

        String folderPath = ROOT_FOLDER + "/" + category + "/" +
                (folderName != null && !folderName.isEmpty() ? folderName + "/" : "");

        StorageReference folderRef = storageRef.child(folderPath);

        folderRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                List<StorageFile> files = new ArrayList<>();

                // Process each file
                for (StorageReference item : listResult.getItems()) {
                    String fileName = item.getName();

                    // Only process PDF files
                    if (fileName.toLowerCase().endsWith(".pdf")) {
                        // Get download URL for each file
                        item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUri) {
                                // Get metadata for additional info
                                item.getMetadata().addOnSuccessListener(metadata -> {
                                    StorageFile file = new StorageFile(
                                            fileName,
                                            downloadUri.toString(),
                                            metadata.getSizeBytes(),
                                            metadata.getCreationTimeMillis()
                                    );
                                    files.add(file);

                                    // Check if we've processed all files
                                    if (files.size() == listResult.getItems().size()) {
                                        Log.d(TAG, "Loaded " + files.size() + " files");
                                        callback.onFilesLoaded(files);
                                    }
                                });
                            }
                        }).addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to get download URL for: " + fileName);
                            // Continue with other files even if one fails
                        });
                    }
                }

                // If no PDF files found
                if (listResult.getItems().isEmpty()) {
                    Log.d(TAG, "No files found in folder");
                    callback.onFilesLoaded(files);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to get files in folder: " + folderName, e);
                callback.onFailure("Failed to load files: " + e.getMessage());
            }
        });
    }

    /**
     * Get files directly in category (not in subfolders)
     */
    public void getFilesInCategory(String category, FilesCallback callback) {
        getFilesInFolder(category, "", callback);
    }

    /**
     * Build storage path for file upload
     */
    private String buildStoragePath(String category, String folderName, String fileName) {
        StringBuilder path = new StringBuilder(ROOT_FOLDER);
        path.append("/").append(category);

        if (folderName != null && !folderName.trim().isEmpty()) {
            path.append("/").append(folderName.trim());
        }

        path.append("/").append(fileName);

        String finalPath = path.toString();
        Log.d(TAG, "Storage path: " + finalPath);
        return finalPath;
    }

    /**
     * Delete a file from Firebase Storage
     */
    public void deleteFile(String category, String folderName, String fileName,
                           OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String storagePath = buildStoragePath(category, folderName, fileName);
        StorageReference fileRef = storageRef.child(storagePath);

        fileRef.delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Get download URL for a specific file
     */
    public void getFileDownloadUrl(String category, String folderName, String fileName,
                                   OnSuccessListener<Uri> onSuccess, OnFailureListener onFailure) {
        String storagePath = buildStoragePath(category, folderName, fileName);
        StorageReference fileRef = storageRef.child(storagePath);

        fileRef.getDownloadUrl()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // Data classes for Storage items
    public static class StorageFolder {
        private String name;
        private String path;

        public StorageFolder(String name, String path) {
            this.name = name;
            this.path = path;
        }

        public String getName() { return name; }
        public String getPath() { return path; }
    }

    public static class StorageFile {
        private String name;
        private String downloadUrl;
        private long size;
        private long createdTime;

        public StorageFile(String name, String downloadUrl, long size, long createdTime) {
            this.name = name;
            this.downloadUrl = downloadUrl;
            this.size = size;
            this.createdTime = createdTime;
        }

        public String getName() { return name; }
        public String getDownloadUrl() { return downloadUrl; }
        public long getSize() { return size; }
        public long getCreatedTime() { return createdTime; }
    }

    /**
     * Test Firebase Storage connection
     */
    public void testStorageConnection(UploadCallback callback) {
        Log.d(TAG, "Testing Firebase Storage connection...");

        // Simple test - try to get the root reference
        try {
            StorageReference testRef = storageRef.child(ROOT_FOLDER);
            testRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                @Override
                public void onSuccess(ListResult listResult) {
                    Log.d(TAG, "Firebase Storage connection successful!");
                    callback.onSuccess("test_connection", "Firebase Storage Connected Successfully");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Firebase Storage connection failed", e);
                    callback.onFailure("Connection failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to test storage connection", e);
            callback.onFailure("Test failed: " + e.getMessage());
        }
    }
}
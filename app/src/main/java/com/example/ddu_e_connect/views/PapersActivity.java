package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.adapters.FolderAdapter;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class PapersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FolderAdapter folderAdapter;
    private List<String> folderList;
    private FirebaseStorage storage;
    private String currentPath;
    private static final String TAG = "PapersActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_papers);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        folderList = new ArrayList<>();
        folderAdapter = new FolderAdapter(folderList, this::onItemClick);
        recyclerView.setAdapter(folderAdapter);

        storage = FirebaseStorage.getInstance();

        // Start fetching folders from 'uploads'
        currentPath = "uploads/";
        loadFolders(currentPath);
    }

    private void loadFolders(String path) {
        Log.d(TAG, "Loading folders and files from path: " + path);

        StorageReference listRef = storage.getReference().child(path);

        listRef.listAll().addOnSuccessListener(listResult -> {
            folderList.clear();  // Clear the list before adding new items
            for (StorageReference prefix : listResult.getPrefixes()) {
                Log.d(TAG, "Found folder: " + prefix.getName());
                String folderName = prefix.getName().replace("/", "");  // Remove trailing '/'
                folderList.add(folderName + "/");  // Append '/' to distinguish folders
            }
            for (StorageReference item : listResult.getItems()) {
                Log.d(TAG, "Found file: " + item.getName());
                String fileName = item.getName().substring(item.getName().lastIndexOf("/") + 1);  // Get only the file name
                folderList.add(fileName);
            }
            folderAdapter.notifyDataSetChanged();
            Log.d(TAG, "Adapter notified, folderList size: " + folderList.size());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load folders.", e);
            Toast.makeText(PapersActivity.this, "Failed to load folders.", Toast.LENGTH_SHORT).show();
        });
    }


    private void onItemClick(String itemName) {
        if (itemName.endsWith("/")) {
            // If the item is a folder, load the contents of the folder
            currentPath = currentPath + itemName;
            loadFolders(currentPath);
        } else if (itemName.endsWith(".pdf")) {
            // If the item is a PDF, open it
            openPdf(currentPath + itemName);
        }
    }

    private void openPdf(String fullPath) {
        StorageReference pdfRef = storage.getReference().child(fullPath);

        pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to open PDF.", e);
            Toast.makeText(PapersActivity.this, "Failed to open PDF.", Toast.LENGTH_SHORT).show();
        });
    }
}

package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.adapters.PapersAdapter;
import com.example.ddu_e_connect.model.FolderModel;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class PapersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PapersAdapter papersAdapter; // Use a single adapter for both folders and PDFs
    private List<FolderModel> folderList = new ArrayList<>();
    private FirebaseStorage storage;
    private String currentPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_papers);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firebase Storage and set initial folder path
        storage = FirebaseStorage.getInstance();
        currentPath = "uploads/";

        // Load the folders and files
        loadFoldersAndFiles(currentPath);
    }

    private void loadFoldersAndFiles(String path) {
        Log.d("PapersActivity", "Loading folders and files from path: " + path);
        StorageReference listRef = storage.getReference().child(path);

        listRef.listAll().addOnSuccessListener(listResult -> {
            folderList.clear();

            // Add folders to folderList
            for (StorageReference folderRef : listResult.getPrefixes()) {
                String folderName = folderRef.getName();
                folderList.add(new FolderModel(folderName, false)); // Add folder as not a PDF
            }

            // Add PDF files to folderList
            for (StorageReference fileRef : listResult.getItems()) {
                String fileName = fileRef.getName();
                if (fileName.endsWith(".pdf")) {
                    folderList.add(new FolderModel(fileName, true)); // Add PDF as true
                }
            }

            // Set up adapter
            setupRecyclerView();
        }).addOnFailureListener(e -> {
            Log.e("PapersActivity", "Failed to load folders.", e);
            Toast.makeText(PapersActivity.this, "Failed to load folders.", Toast.LENGTH_SHORT).show();
        });
    }
    private void setupRecyclerView() {
        papersAdapter = new PapersAdapter(folderList, item -> {
            if (item.isPdf()) {
                openPdf(item.getName());
            } else {
                currentPath += item.getName() + "/";
                loadFoldersAndFiles(currentPath);
            }
        }, this); // Pass the context here
        recyclerView.setAdapter(papersAdapter);
    }



    private void openPdf(String fileName) {
        StorageReference pdfRef = storage.getReference().child(currentPath + fileName);
        pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }).addOnFailureListener(e -> {
            Log.e("PapersActivity", "Failed to open PDF.", e);
            Toast.makeText(PapersActivity.this, "Failed to open PDF.", Toast.LENGTH_SHORT).show();
        });
    }
}

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
import com.example.ddu_e_connect.presentation.ui.adapter.PdfAdapter;
import com.example.ddu_e_connect.data.model.PdfModel;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;

public class PdfListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PdfAdapter pdfAdapter;
    private List<PdfModel> pdfList = new ArrayList<>();
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_list);  // Adjust the layout accordingly
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        storage = FirebaseStorage.getInstance();

        // Initialize the PdfAdapter with the OnPdfClickListener
        pdfAdapter = new PdfAdapter(this, pdfList, this::onPdfClick);
        recyclerView.setAdapter(pdfAdapter);

        loadPdfFiles();  // Load your PDF files
    }

    private void loadPdfFiles() {
        // Load PDF files from Firebase Storage
        StorageReference pdfRef = storage.getReference().child("uploads/"); // Use correct path
        pdfRef.listAll().addOnSuccessListener(listResult -> {
            pdfList.clear();

            // Process each PDF file
            for (StorageReference item : listResult.getItems()) {
                String pdfName = item.getName();

                // Get download URL asynchronously (FIXED!)
                item.getDownloadUrl().addOnSuccessListener(uri -> {
                    String pdfUrl = uri.toString();
                    pdfList.add(new PdfModel(pdfName, pdfUrl));
                    pdfAdapter.notifyDataSetChanged();
                }).addOnFailureListener(e -> {
                    Log.e("PdfListActivity", "Failed to get download URL for: " + pdfName, e);
                });
            }

        }).addOnFailureListener(e -> {
            Log.e("PdfListActivity", "Failed to load PDFs.", e);
            Toast.makeText(this, "Failed to load PDFs.", Toast.LENGTH_SHORT).show();
        });
    }

    private void onPdfClick(PdfModel pdfModel) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(pdfModel.getUrl()), "application/pdf");
        startActivity(intent);
    }
}

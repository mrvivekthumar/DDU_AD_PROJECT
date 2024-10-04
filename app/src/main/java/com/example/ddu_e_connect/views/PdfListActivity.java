package com.example.ddu_e_connect.views;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ddu_e_connect.adapters.PdfAdapter;
import com.example.ddu_e_connect.model.PdfModel;
import com.example.ddu_e_connect.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PdfListActivity extends AppCompatActivity {

    private PdfAdapter pdfAdapter;
    private List<PdfModel> pdfList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private String folderName;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_list); // Set layout using resource ID

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));

        // Initialize views using findViewById
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progressBar);

        firestore = FirebaseFirestore.getInstance();
        folderName = getIntent().getStringExtra("FOLDER_NAME");

        setupRecyclerView();
        loadPdfs();
    }

    private void setupRecyclerView() {
        pdfAdapter = new PdfAdapter(this, pdfList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(pdfAdapter);
    }

    private void loadPdfs() {
        progressBar.setVisibility(View.VISIBLE); // Show loading indicator
        firestore.collection("folders").document(folderName).collection("pdfs").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pdfList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        PdfModel pdf = document.toObject(PdfModel.class);
                        pdfList.add(pdf);
                    }
                    pdfAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE); // Hide loading indicator
                })
                .addOnFailureListener(e -> {
                    Log.e("PdfListActivity", "Failed to load PDFs: " + e.getMessage());
                    progressBar.setVisibility(View.GONE); // Hide loading indicator
                });
    }
}

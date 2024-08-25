package com.example.ddu_e_connect.views;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ddu_e_connect.adapters.PdfAdapter;
import com.example.ddu_e_connect.databinding.ActivityPdfListBinding;
import com.example.ddu_e_connect.model.PdfModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PdfListActivity extends AppCompatActivity {

    private ActivityPdfListBinding binding;
    private PdfAdapter pdfAdapter;
    private List<PdfModel> pdfList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private String folderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();
        folderName = getIntent().getStringExtra("FOLDER_NAME");

        setupRecyclerView();
        loadPdfs();
    }

    private void setupRecyclerView() {
        pdfAdapter = new PdfAdapter(pdfList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(pdfAdapter);
    }

    private void loadPdfs() {
        firestore.collection("folders").document(folderName).collection("pdfs").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pdfList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        PdfModel pdf = document.toObject(PdfModel.class);
                        pdfList.add(pdf);
                    }
                    pdfAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("PdfListActivity", "Failed to load PDFs: " + e.getMessage());
                });
    }
}

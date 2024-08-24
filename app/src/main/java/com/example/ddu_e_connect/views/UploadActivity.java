package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private EditText pdfNameEditText;
    private Button selectPdfButton;
    private Button uploadPdfButton;
    private Uri pdfUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        pdfNameEditText = findViewById(R.id.pdfNameEditText);
        selectPdfButton = findViewById(R.id.selectPdfButton);
        uploadPdfButton = findViewById(R.id.uploadPdfButton);

        selectPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("application/pdf");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_REQUEST);
        });

        uploadPdfButton.setOnClickListener(v -> {
            String pdfName = pdfNameEditText.getText().toString().trim();
            if (pdfUri != null && !pdfName.isEmpty()) {
                uploadPdf(pdfUri, pdfName);
            } else {
                Log.e("UploadActivity", "No PDF selected or PDF name is empty.");
                Toast.makeText(UploadActivity.this, "Please select a PDF and provide a name.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pdfUri = data.getData();
            Log.d("UploadActivity", "PDF selected: " + pdfUri.toString());
            Toast.makeText(this, "PDF selected successfully.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadPdf(Uri pdfUri, String pdfName) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("uploads").child(pdfName + ".pdf");

        storageReference.putFile(pdfUri).addOnSuccessListener(taskSnapshot -> {
            Log.d("UploadActivity", "PDF uploaded successfully.");
            Toast.makeText(UploadActivity.this, "PDF uploaded successfully.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e("UploadActivity", "Failed to upload PDF: " + e.getMessage());
            Toast.makeText(UploadActivity.this, "Failed to upload PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}

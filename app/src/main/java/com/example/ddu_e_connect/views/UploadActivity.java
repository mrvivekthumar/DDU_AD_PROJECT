package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.databinding.ActivityUploadBinding;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UploadActivity extends AppCompatActivity {

    private ActivityUploadBinding binding;
    private Uri pdfUri;

    private ActivityResultLauncher<Intent> pdfActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Register the ActivityResultLauncher
        pdfActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        pdfUri = result.getData().getData();
                        Log.d("UploadActivity", "PDF selected: " + pdfUri.toString());
                        Toast.makeText(this, "PDF selected successfully.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("UploadActivity", "No PDF selected or selection was canceled.");
                        Toast.makeText(this, "PDF selection failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        binding.selectPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pdfActivityResultLauncher.launch(Intent.createChooser(intent, "Select PDF"));
        });

        binding.uploadPdfButton.setOnClickListener(v -> {
            String pdfName = binding.pdfNameEditText.getText().toString().trim();
            String folderName = binding.folderNameEditText.getText().toString().trim();


            binding.uploadPdfButton.setEnabled(false);


            if (pdfUri != null && !pdfName.isEmpty()) {
                uploadPdf(pdfUri, pdfName, folderName);
            } else {
                Log.e("UploadActivity", "No PDF selected or PDF name is empty.");
                Toast.makeText(UploadActivity.this, "Please select a PDF and provide a name.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadPdf(Uri pdfUri, String pdfName, @Nullable String folderName) {
        StorageReference storageReference;
        if (folderName != null && !folderName.isEmpty()) {
            storageReference = FirebaseStorage.getInstance()
                    .getReference("uploads/" + folderName)
                    .child(pdfName + ".pdf");
            Log.d("UploadActivity", "Uploading to path: uploads/" + folderName + "/" + pdfName + ".pdf");
        } else {
            storageReference = FirebaseStorage.getInstance()
                    .getReference("uploads")
                    .child(pdfName + ".pdf");
            Log.d("UploadActivity", "Uploading to path: uploads/" + pdfName + ".pdf");
        }

        storageReference.putFile(pdfUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("UploadActivity", "PDF uploaded successfully.");
                    Toast.makeText(UploadActivity.this, "PDF uploaded successfully.", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    Log.e("UploadActivity", "Failed to upload PDF: " + e.getMessage());
                    Toast.makeText(UploadActivity.this, "Failed to upload PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void navigateToHome() {
        Intent intent = new Intent(UploadActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}

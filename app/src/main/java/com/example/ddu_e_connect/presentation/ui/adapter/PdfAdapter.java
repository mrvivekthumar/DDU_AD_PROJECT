package com.example.ddu_e_connect.presentation.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.data.model.PdfModel;

import java.util.List;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {

    private Context context;
    private List<PdfModel> pdfList;
    private OnPdfClickListener onPdfClickListener;  // Add the OnPdfClickListener

    public interface OnPdfClickListener {
        void onPdfClick(PdfModel pdfModel);
    }

    public PdfAdapter(Context context, List<PdfModel> pdfList, OnPdfClickListener onPdfClickListener) {
        this.context = context;
        this.pdfList = pdfList;
        this.onPdfClickListener = onPdfClickListener;  // Initialize the listener
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        PdfModel pdfModel = pdfList.get(position);
        holder.pdfNameTextView.setText(pdfModel.getName());

        // Set a click listener for the item
        holder.itemView.setOnClickListener(v -> {
            if (onPdfClickListener != null) {
                onPdfClickListener.onPdfClick(pdfModel);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfList.size();
    }

    public static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView pdfNameTextView;

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            pdfNameTextView = itemView.findViewById(R.id.pdfNameTextView);
        }
    }
}

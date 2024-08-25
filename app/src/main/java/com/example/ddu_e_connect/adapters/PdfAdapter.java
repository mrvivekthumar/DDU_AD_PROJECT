package com.example.ddu_e_connect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.model.PdfModel;

import java.util.List;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {

    private Context context;
    private List<String> pdfList;

    public PdfAdapter(Context context, List<String> pdfList) {
        this.context = context;
        this.pdfList = pdfList;
    }

    public PdfAdapter(List<PdfModel> pdfList) {
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        String pdfName = pdfList.get(position);
        holder.pdfNameTextView.setText(pdfName);
    }

    @Override
    public int getItemCount() {
        return pdfList.size();
    }

    public static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView pdfNameTextView;

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            pdfNameTextView = itemView.findViewById(R.id.pdfName);
        }
    }
}

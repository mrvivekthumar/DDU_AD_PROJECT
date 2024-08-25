package com.example.ddu_e_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;

import java.util.List;

// In FolderAdapter.java
public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {
    private List<String> itemList;
    private OnItemClickListener onItemClickListener;

    public FolderAdapter(List<String> itemList, OnItemClickListener onItemClickListener) {
        this.itemList = itemList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = itemList.get(position);
        holder.nameTextView.setText(item);

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String itemName);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.folderNameTextView);
        }
    }
}

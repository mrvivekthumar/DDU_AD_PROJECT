package com.example.ddu_e_connect.presentation.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.data.model.FolderModel;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private List<FolderModel> folderList;
    private OnFolderClickListener onFolderClickListener;

    public interface OnFolderClickListener {
        void onFolderClick(FolderModel folder);
    }

    public FolderAdapter(List<FolderModel> folderList, OnFolderClickListener onFolderClickListener) {
        this.folderList = folderList;
        this.onFolderClickListener = onFolderClickListener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false); // Use the new layout
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        FolderModel folder = folderList.get(position);
        holder.folderNameTextView.setText(folder.getName());

        // Set the icon based on whether it's a PDF or a folder
        if (folder.isPdf()) {
            holder.folderIcon.setImageResource(R.drawable.ic_pdf); // PDF icon
        } else {
            holder.folderIcon.setImageResource(R.drawable.ic_folder); // Folder icon
        }

        holder.itemView.setOnClickListener(v -> {
            if (onFolderClickListener != null) {
                onFolderClickListener.onFolderClick(folder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        public TextView folderNameTextView;
        public ImageView folderIcon;

        public FolderViewHolder(View itemView) {
            super(itemView);
            folderNameTextView = itemView.findViewById(R.id.item_name); // Ensure this matches the new layout
            folderIcon = itemView.findViewById(R.id.item_icon); // Ensure this matches the new layout
        }
    }
}

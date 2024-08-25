package com.example.ddu_e_connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.model.FolderModel;

import java.util.List;

public class PapersAdapter extends RecyclerView.Adapter<PapersAdapter.ViewHolder> {

    private List<FolderModel> folderList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(FolderModel folder);
    }

    public PapersAdapter(List<FolderModel> folderList, OnItemClickListener onItemClickListener) {
        this.folderList = folderList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(com.example.ddu_e_connect.R.layout.item_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderModel folder = folderList.get(position);
        holder.folderNameTextView.setText(folder.getName());

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(folder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView folderNameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            folderNameTextView = itemView.findViewById(R.id.folderNameTextView);
        }
    }
}


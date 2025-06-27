package com.example.ddu_e_connect.presentation.ui.adapter;

import android.content.Context;
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

public class PapersAdapter extends RecyclerView.Adapter<PapersAdapter.ViewHolder> {

    private List<FolderModel> folderList;
    private OnItemClickListener onItemClickListener;
    private Context context;

    public PapersAdapter(List<FolderModel> folderList, OnItemClickListener onItemClickListener, Context context) {
        this.folderList = folderList;
        this.onItemClickListener = onItemClickListener;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderModel folderModel = folderList.get(position);
        holder.nameTextView.setText(folderModel.getName());

        // Set the icon based on whether it's a PDF or a folder
        if (folderModel.isPdf()) {
            holder.iconImageView.setImageResource(R.drawable.ic_pdf); // Replace with your PDF icon resource
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_folder); // Replace with your folder icon resource
        }

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(folderModel));
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(FolderModel item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public ImageView iconImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.item_name);
            iconImageView = itemView.findViewById(R.id.item_icon); // Adjust ID based on your item layout
        }
    }
}

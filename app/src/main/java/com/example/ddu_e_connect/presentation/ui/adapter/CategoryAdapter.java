package com.example.ddu_e_connect.presentation.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private static final String TAG = "CategoryAdapter";

    private List<CategoryItem> categories;
    private Context context;
    private OnCategoryClickListener listener;
    private int lastPosition = -1;

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem category);
    }

    public CategoryAdapter(List<CategoryItem> categories, Context context, OnCategoryClickListener listener) {
        this.categories = categories;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_card, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem category = categories.get(position);

        // Set category data
        holder.categoryTitle.setText(category.getTitle());
        holder.categorySubtitle.setText(category.getSubtitle());
        holder.categoryIcon.setImageResource(category.getIconResource());
        holder.documentCount.setText(category.getDocumentCount() + " documents");
        holder.lastUpdated.setText(category.getLastUpdated());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Add click animation
                addClickAnimation(v);
                listener.onCategoryClick(category);
            }
        });

        // Apply entrance animation
        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    /**
     * Add click animation
     */
    private void addClickAnimation(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    /**
     * Set entrance animation
     */
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            animation.setDuration(300);
            animation.setStartOffset(position * 50);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull CategoryViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTitle, categorySubtitle, documentCount, lastUpdated;
        ImageView categoryIcon;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.categoryTitle);
            categorySubtitle = itemView.findViewById(R.id.categorySubtitle);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            documentCount = itemView.findViewById(R.id.documentCount);
            lastUpdated = itemView.findViewById(R.id.lastUpdated);
        }
    }

    // Category data class
    public static class CategoryItem {
        private String title;
        private String subtitle;
        private String categoryKey;
        private int iconResource;
        private int documentCount;
        private String lastUpdated;

        public CategoryItem(String title, String subtitle, String categoryKey, int iconResource) {
            this.title = title;
            this.subtitle = subtitle;
            this.categoryKey = categoryKey;
            this.iconResource = iconResource;
            this.documentCount = 0;
            this.lastUpdated = "No documents yet";
        }

        // Getters and setters
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getCategoryKey() { return categoryKey; }
        public int getIconResource() { return iconResource; }
        public int getDocumentCount() { return documentCount; }
        public String getLastUpdated() { return lastUpdated; }

        public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
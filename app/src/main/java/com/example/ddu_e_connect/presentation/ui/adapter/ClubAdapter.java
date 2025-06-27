package com.example.ddu_e_connect.presentation.ui.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.data.model.ClubsModel;

import java.util.ArrayList;

public class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ViewHolder> {
    private static final String TAG = "ClubAdapter";

    private ArrayList<ClubsModel> clubsList;
    private Context context;
    private int lastPosition = -1;

    public ClubAdapter(ArrayList<ClubsModel> clubsList, Context context) {
        this.clubsList = clubsList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_design, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        ClubsModel club = clubsList.get(position);

        // Set basic club information
        holder.clubName.setText(club.getClubName());
        holder.clubLogo.setImageResource(club.getClubLogo());

        // Initially hide expanded content
        holder.clubDescription.setVisibility(View.GONE);
        holder.socialMediaSection.setVisibility(View.GONE);

        // Reset expand indicator rotation
        holder.expandIndicator.setRotation(0f);

        // Set up main card click listener for expand/collapse
        holder.itemView.setOnClickListener(v -> {
            toggleExpandCollapse(holder, club);
        });

        // Set up social media click listeners
        setupSocialMediaClickListeners(holder, club);

        // Apply entrance animations
        setAnimation(holder.itemView, position);

        Log.d(TAG, "Bound club: " + club.getClubName() + " at position " + position);
    }

    /**
     * Toggle expand/collapse functionality
     */
    private void toggleExpandCollapse(ViewHolder holder, ClubsModel club) {
        boolean isExpanded = holder.clubDescription.getVisibility() == View.VISIBLE;

        if (isExpanded) {
            // Collapse
            collapseClub(holder);
        } else {
            // Expand
            expandClub(holder, club);
        }
    }

    /**
     * Expand club details
     */
    private void expandClub(ViewHolder holder, ClubsModel club) {
        // Set description text
        holder.clubDescription.setText(club.getClubDescription());

        // Show expanded content with animation
        holder.clubDescription.setVisibility(View.VISIBLE);
        holder.socialMediaSection.setVisibility(View.VISIBLE);

        // Animate expand indicator rotation
        ObjectAnimator.ofFloat(holder.expandIndicator, "rotation", 0f, 180f)
                .setDuration(300)
                .start();

        // Animate content appearance
        Animation fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        fadeIn.setDuration(300);
        holder.clubDescription.startAnimation(fadeIn);
        holder.socialMediaSection.startAnimation(fadeIn);

        // Add haptic feedback
        holder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);

        Log.d(TAG, "Expanded club: " + club.getClubName());
    }

    /**
     * Collapse club details
     */
    private void collapseClub(ViewHolder holder) {
        // Animate expand indicator rotation
        ObjectAnimator.ofFloat(holder.expandIndicator, "rotation", 180f, 0f)
                .setDuration(300)
                .start();

        // Animate content disappearance
        Animation fadeOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        fadeOut.setDuration(200);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                holder.clubDescription.setVisibility(View.GONE);
                holder.socialMediaSection.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        holder.clubDescription.startAnimation(fadeOut);
        holder.socialMediaSection.startAnimation(fadeOut);

        Log.d(TAG, "Collapsed club details");
    }

    /**
     * Setup social media click listeners
     */
    private void setupSocialMediaClickListeners(ViewHolder holder, ClubsModel club) {
        // LinkedIn click listener
        holder.linkedInIcon.setOnClickListener(v -> {
            addClickAnimation(v);
            openLink(club.getLinkedInUrl(), "LinkedIn");
        });

        // Instagram click listener
        holder.instagramIcon.setOnClickListener(v -> {
            addClickAnimation(v);
            openLink(club.getInstagramUrl(), "Instagram");
        });

        // Website click listener
        holder.websiteIcon.setOnClickListener(v -> {
            addClickAnimation(v);
            openLink(club.getWebsiteUrl(), "Website");
        });
    }

    /**
     * Add click animation to social media buttons
     */
    private void addClickAnimation(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
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
     * Open social media links
     */
    private void openLink(String url, String platform) {
        if (url != null && !url.trim().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);

                showSuccess("Opening " + platform + "...");
                Log.d(TAG, "Opened " + platform + " link: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open " + platform + " link", e);
                showError("Failed to open " + platform);
            }
        } else {
            showError(platform + " link not available");
            Log.w(TAG, platform + " URL is empty or null");
        }
    }

    /**
     * Set entrance animation for items
     */
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation;

            // Alternate between left and right slide animations
            if (position % 2 == 0) {
                animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
            } else {
                animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
            }

            animation.setDuration(600);
            animation.setStartOffset(position * 100); // Staggered animation
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    /**
     * Clear animation on view recycled
     */
    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return clubsList.size();
    }

    /**
     * Enhanced ViewHolder with all new components
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView clubName, clubDescription;
        ImageView clubLogo, expandIndicator;
        LinearLayout socialMediaSection;
        View linkedInIcon, instagramIcon, websiteIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Basic elements
            clubName = itemView.findViewById(R.id.club_name);
            clubDescription = itemView.findViewById(R.id.club_description);
            clubLogo = itemView.findViewById(R.id.club_logo);
            expandIndicator = itemView.findViewById(R.id.expandIndicator);

            // Sections
            socialMediaSection = itemView.findViewById(R.id.social_media_section);

            // Social media icons (now card views with click areas)
            linkedInIcon = (View) itemView.findViewById(R.id.linkedin_icon).getParent();
            instagramIcon = (View) itemView.findViewById(R.id.instagram_icon).getParent();
            websiteIcon = (View) itemView.findViewById(R.id.website_icon).getParent();
        }
    }
}
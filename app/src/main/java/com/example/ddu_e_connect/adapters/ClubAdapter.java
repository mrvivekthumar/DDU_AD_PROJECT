package com.example.ddu_e_connect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.model.ClubsModel;

import java.util.ArrayList;

public class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ViewHolder> {

    private ArrayList<ClubsModel> arrayList;
    private Context context;
    private int lastPosition = -1;

    public ClubAdapter(ArrayList<ClubsModel> arrayList, Context context) {
        this.arrayList = arrayList;
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
        ClubsModel model = arrayList.get(position);
        holder.clubName.setText(model.getClubName());
        holder.clubLogo.setImageResource(model.getClubLogo());

        holder.itemView.setOnClickListener(v -> {
            if (holder.clubDescription.getVisibility() == View.GONE) {
                holder.clubDescription.setVisibility(View.VISIBLE);
                holder.clubDescription.setText(model.getClubDescription());
            } else {
                holder.clubDescription.setVisibility(View.GONE);
            }
        });

        // Apply slide_in_left animation
        // Apply animations
        if (position > lastPosition) {
            if (position % 2 != 0) {
                holder.itemView.setAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.slide_in_left));
            } else {
                holder.itemView.setAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.slide_in_right));
            }
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView clubName, clubDescription;
        ImageView clubLogo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            clubName = itemView.findViewById(R.id.club_name);
            clubDescription = itemView.findViewById(R.id.club_description);
            clubLogo = itemView.findViewById(R.id.club_logo);
        }
    }
}

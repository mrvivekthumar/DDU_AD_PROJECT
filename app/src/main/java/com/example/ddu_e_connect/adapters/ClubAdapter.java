package com.example.ddu_e_connect.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.model.Model;
import com.example.ddu_e_connect.R;

import java.util.ArrayList;

public class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ViewHolder> {

    ArrayList<Model> arrayList;
    Context context;

    public ClubAdapter(ArrayList<Model> arrayList, Context context) {
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
        Model model = arrayList.get(position);
        holder.clubName.setText(model.getClubName());
        holder.clubLogo.setImageResource(model.getClubLogo());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.clubDescription.getVisibility() == View.GONE) {
                    holder.clubDescription.setVisibility(View.VISIBLE);
                    holder.clubDescription.setText(model.getClubDescription());
                } else {
                    holder.clubDescription.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

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

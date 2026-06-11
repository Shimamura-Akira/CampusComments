package com.example.campuscomments.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscomments.AppConstants;
import com.example.campuscomments.R;
import com.example.campuscomments.model.CampusPoi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PoiAdapter extends RecyclerView.Adapter<PoiAdapter.PoiViewHolder> {
    public interface OnPoiClickListener {
        void onPoiClick(CampusPoi poi);
    }

    private final List<CampusPoi> pois = new ArrayList<>();
    private final OnPoiClickListener listener;

    public PoiAdapter(OnPoiClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CampusPoi> data) {
        pois.clear();
        if (data != null) {
            pois.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PoiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poi, parent, false);
        return new PoiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PoiViewHolder holder, int position) {
        CampusPoi poi = pois.get(position);
        holder.nameText.setText(poi.getName());
        holder.metaText.setText(AppConstants.displayType(poi.getType()) + " · " + nullToDash(poi.getAddress()));
        double score = poi.getAvgScore() == null ? 0 : poi.getAvgScore();
        int count = poi.getReviewCount() == null ? 0 : poi.getReviewCount();
        holder.scoreText.setText(String.format(Locale.CHINA, "%.1f 分 · %d 条测评", score, count));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPoiClick(poi);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pois.size();
    }

    private String nullToDash(String value) {
        return value == null || value.trim().isEmpty() ? "暂无地址" : value;
    }

    static class PoiViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView metaText;
        final TextView scoreText;

        PoiViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.poiNameText);
            metaText = itemView.findViewById(R.id.poiMetaText);
            scoreText = itemView.findViewById(R.id.poiScoreText);
        }
    }
}

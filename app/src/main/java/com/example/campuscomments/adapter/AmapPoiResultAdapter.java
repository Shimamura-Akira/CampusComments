package com.example.campuscomments.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.core.PoiItem;
import com.example.campuscomments.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AmapPoiResultAdapter extends RecyclerView.Adapter<AmapPoiResultAdapter.ResultViewHolder> {
    public interface OnResultClickListener {
        void onResultClick(PoiItem item);
    }

    private final List<PoiItem> items = new ArrayList<>();
    private final OnResultClickListener listener;

    public AmapPoiResultAdapter(OnResultClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PoiItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_amap_poi_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        PoiItem item = items.get(position);
        holder.nameText.setText(nonEmpty(item.getTitle(), "未命名地点"));
        holder.addressText.setText(nonEmpty(item.getSnippet(), "暂无详细地址"));
        holder.areaText.setText(areaText(item));
        holder.distanceText.setText(formatDistance(item.getDistance()));
        holder.itemView.setContentDescription(
                holder.nameText.getText() + "，距离" + holder.distanceText.getText());
        holder.itemView.setOnClickListener(v -> listener.onResultClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String areaText(PoiItem item) {
        String adName = nonEmpty(item.getAdName(), "");
        String businessArea = nonEmpty(item.getBusinessArea(), "");
        if (!adName.isEmpty() && !businessArea.isEmpty()) {
            return adName + " · " + businessArea;
        }
        if (!adName.isEmpty()) return adName;
        if (!businessArea.isEmpty()) return businessArea;
        return "北京市";
    }

    private String formatDistance(int distanceMeters) {
        if (distanceMeters <= 0) return "距离未知";
        if (distanceMeters < 1000) return distanceMeters + " m";
        return String.format(Locale.CHINA, "%.1f km", distanceMeters / 1000.0);
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView distanceText;
        final TextView addressText;
        final TextView areaText;

        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.amapPoiNameText);
            distanceText = itemView.findViewById(R.id.amapPoiDistanceText);
            addressText = itemView.findViewById(R.id.amapPoiAddressText);
            areaText = itemView.findViewById(R.id.amapPoiAreaText);
        }
    }
}

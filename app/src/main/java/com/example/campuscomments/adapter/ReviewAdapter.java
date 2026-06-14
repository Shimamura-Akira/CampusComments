package com.example.campuscomments.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscomments.CampusUser;
import com.example.campuscomments.R;
import com.example.campuscomments.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    public interface OnDeleteClickListener {
        void onDeleteClick(Review review);
    }

    private final List<Review> reviews = new ArrayList<>();
    private final String currentUserId;
    private final OnDeleteClickListener deleteListener;

    public ReviewAdapter() {
        this(null, null);
    }

    public ReviewAdapter(String currentUserId, OnDeleteClickListener deleteListener) {
        this.currentUserId = currentUserId;
        this.deleteListener = deleteListener;
    }

    public void submitList(List<Review> data) {
        reviews.clear();
        if (data != null) {
            reviews.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        CampusUser author = review.getAuthor();
        String userName = author == null ? "匿名同学" : firstNonEmpty(author.getNickname(), author.getUsername(), "匿名同学");
        holder.authorText.setText(userName);
        holder.scoreText.setText(safeScore(review.getScore()) + " 分");
        if (review.getPoi() != null && review.getPoi().getName() != null && !review.getPoi().getName().trim().isEmpty()) {
            holder.poiText.setText(review.getPoi().getName());
            holder.poiText.setVisibility(View.VISIBLE);
        } else {
            holder.poiText.setVisibility(View.GONE);
        }
        holder.contentText.setText(firstNonEmpty(review.getContent(), "暂无文字测评"));
        holder.tagText.setText(firstNonEmpty(review.getTags(), "无标签"));
        boolean canDelete = deleteListener != null
                && currentUserId != null
                && author != null
                && currentUserId.equals(author.getObjectId());
        holder.deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        holder.deleteButton.setOnClickListener(canDelete ? v -> deleteListener.onDeleteClick(review) : null);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    private int safeScore(Integer score) {
        return score == null ? 0 : score;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView authorText;
        final TextView scoreText;
        final TextView poiText;
        final TextView contentText;
        final TextView tagText;
        final ImageButton deleteButton;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.reviewAuthorText);
            scoreText = itemView.findViewById(R.id.reviewScoreText);
            poiText = itemView.findViewById(R.id.reviewPoiText);
            contentText = itemView.findViewById(R.id.reviewContentText);
            tagText = itemView.findViewById(R.id.reviewTagText);
            deleteButton = itemView.findViewById(R.id.reviewDeleteButton);
        }
    }
}

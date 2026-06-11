package com.example.campuscomments.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscomments.CampusUser;
import com.example.campuscomments.R;
import com.example.campuscomments.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private final List<Review> reviews = new ArrayList<>();

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
        holder.contentText.setText(firstNonEmpty(review.getContent(), "暂无文字测评"));
        holder.tagText.setText(firstNonEmpty(review.getTags(), "无标签"));
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
        final TextView contentText;
        final TextView tagText;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.reviewAuthorText);
            scoreText = itemView.findViewById(R.id.reviewScoreText);
            contentText = itemView.findViewById(R.id.reviewContentText);
            tagText = itemView.findViewById(R.id.reviewTagText);
        }
    }
}

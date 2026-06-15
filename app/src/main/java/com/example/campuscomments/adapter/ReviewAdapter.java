package com.example.campuscomments.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscomments.CampusUser;
import com.example.campuscomments.R;
import com.example.campuscomments.model.Review;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    public interface OnDeleteClickListener {
        void onDeleteClick(Review review);
    }

    public interface OnReviewClickListener {
        void onReviewClick(Review review);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Review review);
    }

    private final List<Review> reviews = new ArrayList<>();
    private final Set<String> favoriteReviewIds = new HashSet<>();
    private final Set<String> pendingFavoriteIds = new HashSet<>();
    private final String currentUserId;
    private final OnDeleteClickListener deleteListener;
    private final OnReviewClickListener reviewClickListener;
    private final OnFavoriteClickListener favoriteClickListener;

    public ReviewAdapter() {
        this(null, null, null, null);
    }

    public ReviewAdapter(String currentUserId, OnDeleteClickListener deleteListener) {
        this(currentUserId, deleteListener, null, null);
    }

    public ReviewAdapter(String currentUserId,
                         OnDeleteClickListener deleteListener,
                         OnReviewClickListener reviewClickListener,
                         OnFavoriteClickListener favoriteClickListener) {
        this.currentUserId = currentUserId;
        this.deleteListener = deleteListener;
        this.reviewClickListener = reviewClickListener;
        this.favoriteClickListener = favoriteClickListener;
    }

    public void submitList(List<Review> data) {
        reviews.clear();
        if (data != null) {
            reviews.addAll(data);
        }
        notifyDataSetChanged();
    }

    public void setFavoriteReviewIds(Set<String> reviewIds) {
        favoriteReviewIds.clear();
        if (reviewIds != null) {
            favoriteReviewIds.addAll(reviewIds);
        }
        pendingFavoriteIds.clear();
        notifyDataSetChanged();
    }

    public void setFavoriteState(String reviewId, boolean favorite) {
        setFavoriteState(reviewId, favorite, null);
    }

    public void setFavoriteState(String reviewId, boolean favorite, Integer favoriteCount) {
        if (reviewId == null) {
            return;
        }
        if (favorite) {
            favoriteReviewIds.add(reviewId);
        } else {
            favoriteReviewIds.remove(reviewId);
        }
        for (Review review : reviews) {
            if (reviewId.equals(review.getObjectId()) && favoriteCount != null) {
                review.setFavoriteCount(Math.max(0, favoriteCount));
            }
        }
        pendingFavoriteIds.remove(reviewId);
        notifyReviewChanged(reviewId);
    }

    public void clearFavoritePending(String reviewId) {
        if (reviewId == null) {
            return;
        }
        pendingFavoriteIds.remove(reviewId);
        notifyReviewChanged(reviewId);
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
        String reviewId = review.getObjectId();
        CampusUser author = review.getAuthor();
        String userName = author == null
                ? "匿名同学"
                : firstNonEmpty(author.getNickname(), author.getUsername(), "匿名同学");
        holder.authorText.setText(userName);
        holder.scoreText.setText(safeScore(review.getScore()) + " 分");

        if (review.getPoi() != null
                && review.getPoi().getName() != null
                && !review.getPoi().getName().trim().isEmpty()) {
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
        holder.deleteButton.setOnClickListener(
                canDelete ? v -> deleteListener.onDeleteClick(review) : null);

        boolean canFavorite = favoriteClickListener != null && reviewId != null;
        boolean isFavorite = canFavorite && favoriteReviewIds.contains(reviewId);
        holder.favoriteButton.setVisibility(canFavorite ? View.VISIBLE : View.GONE);
        holder.favoriteButton.setSelected(isFavorite);
        holder.favoriteButton.setImageResource(
                isFavorite ? R.drawable.ic_heart_24 : R.drawable.ic_heart_outline_24);
        holder.favoriteButton.setContentDescription(isFavorite ? "取消收藏测评" : "收藏测评");
        int favoriteCount = review.getFavoriteCount() == null
                ? 0
                : Math.max(0, review.getFavoriteCount());
        holder.favoriteCountText.setText(String.valueOf(favoriteCount));
        holder.favoriteCountText.setContentDescription(favoriteCount + " 人收藏");
        holder.favoriteButton.setEnabled(
                canFavorite && !pendingFavoriteIds.contains(reviewId));
        holder.favoriteButton.setOnClickListener(canFavorite ? v -> {
            pendingFavoriteIds.add(reviewId);
            holder.favoriteButton.setEnabled(false);
            favoriteClickListener.onFavoriteClick(review);
        } : null);

        boolean canOpen = reviewClickListener != null && review.getPoi() != null;
        holder.itemView.setClickable(canOpen);
        holder.itemView.setFocusable(canOpen);
        holder.itemView.setContentDescription(
                canOpen ? "在地图中查看这条测评对应的地点" : null);
        holder.itemView.setOnClickListener(
                canOpen ? v -> reviewClickListener.onReviewClick(review) : null);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    private void notifyReviewChanged(String reviewId) {
        for (int i = 0; i < reviews.size(); i++) {
            if (reviewId.equals(reviews.get(i).getObjectId())) {
                notifyItemChanged(i);
            }
        }
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
        final TextView favoriteCountText;
        final ImageButton favoriteButton;
        final ImageButton deleteButton;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.reviewAuthorText);
            scoreText = itemView.findViewById(R.id.reviewScoreText);
            poiText = itemView.findViewById(R.id.reviewPoiText);
            contentText = itemView.findViewById(R.id.reviewContentText);
            tagText = itemView.findViewById(R.id.reviewTagText);
            favoriteCountText = itemView.findViewById(R.id.reviewFavoriteCountText);
            favoriteButton = itemView.findViewById(R.id.reviewFavoriteButton);
            deleteButton = itemView.findViewById(R.id.reviewDeleteButton);
        }
    }
}

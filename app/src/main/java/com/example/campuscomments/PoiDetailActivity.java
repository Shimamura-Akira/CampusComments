package com.example.campuscomments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscomments.adapter.ReviewAdapter;
import com.example.campuscomments.model.CampusPoi;
import com.example.campuscomments.model.Favorite;
import com.example.campuscomments.model.Review;
import com.example.campuscomments.util.ReviewDeleteUtils;
import com.example.campuscomments.util.ReviewFavoriteUtils;
import com.example.campuscomments.util.WindowInsetUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.QueryListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;

public class PoiDetailActivity extends AppCompatActivity {
    private String poiObjectId;
    private CampusPoi currentPoi;
    private Favorite currentFavorite;
    private ReviewAdapter reviewAdapter;
    private ProgressBar progressBar;
    private TextView titleText;
    private TextView typeText;
    private TextView addressText;
    private TextView descriptionText;
    private TextView scoreText;
    private TextView locationText;
    private TextView emptyReviewText;
    private Button favoriteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_detail);
        WindowInsetUtils.applySystemBars(this);
        WindowInsetUtils.bindBackButton(this);

        poiObjectId = getIntent().getStringExtra(AppConstants.EXTRA_POI_OBJECT_ID);
        if (TextUtils.isEmpty(poiObjectId)) {
            Toast.makeText(this, "缺少兴趣点 ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar = findViewById(R.id.progressBar);
        titleText = findViewById(R.id.titleText);
        typeText = findViewById(R.id.typeText);
        addressText = findViewById(R.id.addressText);
        descriptionText = findViewById(R.id.descriptionText);
        scoreText = findViewById(R.id.scoreText);
        locationText = findViewById(R.id.locationText);
        emptyReviewText = findViewById(R.id.emptyReviewText);
        favoriteButton = findViewById(R.id.favoriteButton);
        Button reviewButton = findViewById(R.id.reviewButton);
        RecyclerView reviewRecyclerView = findViewById(R.id.reviewRecyclerView);

        CampusUser currentUser = BmobUser.getCurrentUser(CampusUser.class);
        reviewAdapter = new ReviewAdapter(
                currentUser == null ? null : currentUser.getObjectId(),
                this::confirmDeleteReview,
                null,
                this::toggleReviewFavorite);
        reviewRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewRecyclerView.setAdapter(reviewAdapter);

        reviewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewEditActivity.class);
            intent.putExtra(AppConstants.EXTRA_POI_OBJECT_ID, poiObjectId);
            startActivity(intent);
        });
        favoriteButton.setOnClickListener(v -> toggleFavorite());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPoi();
        loadReviews();
        loadFavoriteState();
        loadReviewFavoriteStates();
    }

    private void loadPoi() {
        progressBar.setVisibility(View.VISIBLE);
        BmobQuery<CampusPoi> query = new BmobQuery<>();
        query.getObject(poiObjectId, new QueryListener<CampusPoi>() {
            @Override
            public void done(CampusPoi poi, BmobException e) {
                progressBar.setVisibility(View.GONE);
                if (e == null) {
                    currentPoi = poi;
                    bindPoi(poi);
                } else {
                    Toast.makeText(PoiDetailActivity.this, "加载详情失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void loadReviews() {
        CampusPoi ref = new CampusPoi();
        ref.setObjectId(poiObjectId);
        BmobQuery<Review> query = new BmobQuery<>();
        query.addWhereEqualTo("poi", ref);
        query.include("author,poi");
        query.order("-createdAt");
        query.findObjects(new FindListener<Review>() {
            @Override
            public void done(List<Review> reviews, BmobException e) {
                if (e == null) {
                    reviewAdapter.submitList(reviews);
                    emptyReviewText.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(PoiDetailActivity.this, "加载测评失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void confirmDeleteReview(Review review) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除测评")
                .setMessage("删除后无法恢复，确定删除这条测评吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) ->
                        ReviewDeleteUtils.deleteOwnedReview(review, (deleted, errorMessage) -> {
                            if (deleted) {
                                Toast.makeText(this, "测评已删除", Toast.LENGTH_SHORT).show();
                                loadReviews();
                                loadPoi();
                            }
                            if (errorMessage != null) {
                                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    private void loadFavoriteState() {
        CampusUser user = BmobUser.getCurrentUser(CampusUser.class);
        if (user == null) return;
        CampusPoi ref = new CampusPoi();
        ref.setObjectId(poiObjectId);
        BmobQuery<Favorite> query = new BmobQuery<>();
        query.addWhereEqualTo("user", user);
        query.addWhereEqualTo("poi", ref);
        query.findObjects(new FindListener<Favorite>() {
            @Override
            public void done(List<Favorite> favorites, BmobException e) {
                if (e == null && !favorites.isEmpty()) {
                    currentFavorite = favorites.get(0);
                    favoriteButton.setText("取消收藏");
                } else {
                    currentFavorite = null;
                    favoriteButton.setText("收藏");
                }
            }
        });
    }

    private void loadReviewFavoriteStates() {
        CampusUser user = BmobUser.getCurrentUser(CampusUser.class);
        if (user == null) {
            return;
        }
        ReviewFavoriteUtils.loadFavoriteReviewIds(user, (reviewIds, errorMessage) -> {
            if (errorMessage == null) {
                reviewAdapter.setFavoriteReviewIds(reviewIds);
            }
        });
    }

    private void toggleReviewFavorite(Review review) {
        CampusUser user = BmobUser.getCurrentUser(CampusUser.class);
        ReviewFavoriteUtils.toggleFavorite(user, review, (favorite, favoriteCount, errorMessage) -> {
            String reviewId = review == null ? null : review.getObjectId();
            if (errorMessage != null) {
                reviewAdapter.clearFavoritePending(reviewId);
                Toast.makeText(this, "操作失败：" + errorMessage, Toast.LENGTH_SHORT).show();
                return;
            }
            reviewAdapter.setFavoriteState(reviewId, favorite, favoriteCount);
            Toast.makeText(
                    this,
                    favorite ? "已收藏测评" : "已取消收藏",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void bindPoi(CampusPoi poi) {
        titleText.setText(poi.getName());
        typeText.setText("类型：" + AppConstants.displayType(poi.getType()));
        addressText.setText("地址：" + safeText(poi.getAddress(), "暂无地址"));
        descriptionText.setText("描述：" + safeText(poi.getDescription(), "暂无描述"));
        double score = poi.getAvgScore() == null ? 0 : poi.getAvgScore();
        int count = poi.getReviewCount() == null ? 0 : poi.getReviewCount();
        scoreText.setText(String.format(Locale.CHINA, "评分：%.1f 分 · %d 条测评", score, count));
        locationText.setText(String.format(Locale.CHINA, "经纬度：%.6f, %.6f",
                poi.getLatitude() == null ? 0 : poi.getLatitude(),
                poi.getLongitude() == null ? 0 : poi.getLongitude()));
    }

    private void toggleFavorite() {
        CampusUser user = BmobUser.getCurrentUser(CampusUser.class);
        if (user == null || currentPoi == null) return;
        favoriteButton.setEnabled(false);
        if (currentFavorite == null) {
            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setPoi(currentPoi);
            favorite.save(new SaveListener<String>() {
                @Override
                public void done(String objectId, BmobException e) {
                    favoriteButton.setEnabled(true);
                    if (e == null) {
                        Toast.makeText(PoiDetailActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                        loadFavoriteState();
                    } else {
                        Toast.makeText(PoiDetailActivity.this, "收藏失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            currentFavorite.delete(new UpdateListener() {
                @Override
                public void done(BmobException e) {
                    favoriteButton.setEnabled(true);
                    if (e == null) {
                        Toast.makeText(PoiDetailActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                        loadFavoriteState();
                    } else {
                        Toast.makeText(PoiDetailActivity.this, "取消失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}

package com.example.campuscomments;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscomments.adapter.PoiAdapter;
import com.example.campuscomments.adapter.ReviewAdapter;
import com.example.campuscomments.model.Favorite;
import com.example.campuscomments.model.Review;
import com.example.campuscomments.util.WindowInsetUtils;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

public class MyActivity extends AppCompatActivity {
    private CampusUser currentUser;
    private PoiAdapter favoriteAdapter;
    private ReviewAdapter reviewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        WindowInsetUtils.applySystemBars(this);

        currentUser = BmobUser.getCurrentUser(CampusUser.class);
        if (currentUser == null) {
            finish();
            return;
        }

        TextView userText = findViewById(R.id.userText);
        RecyclerView favoriteRecyclerView = findViewById(R.id.favoriteRecyclerView);
        RecyclerView reviewRecyclerView = findViewById(R.id.reviewRecyclerView);
        Button logoutButton = findViewById(R.id.logoutButton);

        String displayName = currentUser.getNickname() == null ? currentUser.getUsername() : currentUser.getNickname();
        userText.setText("当前用户：" + displayName);
        favoriteAdapter = new PoiAdapter(poi -> {
            Intent intent = new Intent(this, PoiDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_POI_OBJECT_ID, poi.getObjectId());
            startActivity(intent);
        });
        reviewAdapter = new ReviewAdapter();
        favoriteRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoriteRecyclerView.setAdapter(favoriteAdapter);
        reviewRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewRecyclerView.setAdapter(reviewAdapter);

        logoutButton.setOnClickListener(v -> {
            BmobUser.logOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
        loadReviews();
    }

    private void loadFavorites() {
        BmobQuery<Favorite> query = new BmobQuery<>();
        query.addWhereEqualTo("user", currentUser);
        query.include("poi");
        query.order("-createdAt");
        query.findObjects(new FindListener<Favorite>() {
            @Override
            public void done(List<Favorite> favorites, BmobException e) {
                if (e == null) {
                    List<com.example.campuscomments.model.CampusPoi> pois = new ArrayList<>();
                    for (Favorite favorite : favorites) {
                        if (favorite.getPoi() != null) {
                            pois.add(favorite.getPoi());
                        }
                    }
                    favoriteAdapter.submitList(pois);
                } else {
                    Toast.makeText(MyActivity.this, "加载收藏失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadReviews() {
        BmobQuery<Review> query = new BmobQuery<>();
        query.addWhereEqualTo("author", currentUser);
        query.include("author,poi");
        query.order("-createdAt");
        query.findObjects(new FindListener<Review>() {
            @Override
            public void done(List<Review> reviews, BmobException e) {
                if (e == null) {
                    reviewAdapter.submitList(reviews);
                } else {
                    Toast.makeText(MyActivity.this, "加载测评失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

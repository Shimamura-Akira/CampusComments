package com.example.campuscomments;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuscomments.model.CampusPoi;
import com.example.campuscomments.model.Review;
import com.example.campuscomments.util.WindowInsetUtils;

import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;

public class ReviewEditActivity extends AppCompatActivity {
    private String poiObjectId;
    private RatingBar ratingBar;
    private EditText contentInput;
    private EditText tagsInput;
    private Button publishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_edit);
        WindowInsetUtils.applySystemBars(this);

        poiObjectId = getIntent().getStringExtra(AppConstants.EXTRA_POI_OBJECT_ID);
        if (TextUtils.isEmpty(poiObjectId)) {
            Toast.makeText(this, "缺少兴趣点 ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ratingBar = findViewById(R.id.ratingBar);
        contentInput = findViewById(R.id.contentInput);
        tagsInput = findViewById(R.id.tagsInput);
        publishButton = findViewById(R.id.publishButton);
        publishButton.setOnClickListener(v -> publishReview());
    }

    private void publishReview() {
        int score = Math.round(ratingBar.getRating());
        String content = contentInput.getText().toString().trim();
        String tags = tagsInput.getText().toString().trim();
        CampusUser user = BmobUser.getCurrentUser(CampusUser.class);

        if (user == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        if (score <= 0) {
            Toast.makeText(this, "请选择评分", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请输入测评内容", Toast.LENGTH_SHORT).show();
            return;
        }

        publishButton.setEnabled(false);
        saveReview(score, content, tags, user);
    }

    private void saveReview(int score, String content, String tags, CampusUser user) {
        CampusPoi poi = new CampusPoi();
        poi.setObjectId(poiObjectId);

        Review review = new Review();
        review.setPoi(poi);
        review.setAuthor(user);
        review.setScore(score);
        review.setContent(content);
        review.setTags(tags);
        review.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                if (e == null) {
                    recalculatePoiScore();
                } else {
                    publishButton.setEnabled(true);
                    Toast.makeText(ReviewEditActivity.this, "发布失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void recalculatePoiScore() {
        CampusPoi poi = new CampusPoi();
        poi.setObjectId(poiObjectId);
        BmobQuery<Review> query = new BmobQuery<>();
        query.addWhereEqualTo("poi", poi);
        query.findObjects(new FindListener<Review>() {
            @Override
            public void done(List<Review> reviews, BmobException e) {
                if (e == null) {
                    double total = 0;
                    for (Review review : reviews) {
                        total += review.getScore() == null ? 0 : review.getScore();
                    }
                    double avg = reviews.isEmpty() ? 0 : total / reviews.size();
                    CampusPoi update = new CampusPoi();
                    update.setAvgScore(avg);
                    update.setReviewCount(reviews.size());
                    update.update(poiObjectId, new UpdateListener() {
                        @Override
                        public void done(BmobException e) {
                            publishButton.setEnabled(true);
                            if (e == null) {
                                Toast.makeText(ReviewEditActivity.this, "测评已发布", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ReviewEditActivity.this, "评分更新失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    publishButton.setEnabled(true);
                    Toast.makeText(ReviewEditActivity.this, "评分统计失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

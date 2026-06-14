package com.example.campuscomments.util;

import com.example.campuscomments.CampusUser;
import com.example.campuscomments.model.CampusPoi;
import com.example.campuscomments.model.Review;

import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;

public final class ReviewDeleteUtils {
    public interface Callback {
        void onComplete(boolean deleted, String errorMessage);
    }

    private ReviewDeleteUtils() {
    }

    public static void deleteOwnedReview(Review review, Callback callback) {
        CampusUser currentUser = BmobUser.getCurrentUser(CampusUser.class);
        if (currentUser == null
                || review == null
                || review.getAuthor() == null
                || currentUser.getObjectId() == null
                || !currentUser.getObjectId().equals(review.getAuthor().getObjectId())) {
            callback.onComplete(false, "只能删除自己发表的测评");
            return;
        }

        String poiObjectId = review.getPoi() == null ? null : review.getPoi().getObjectId();
        if (poiObjectId == null || poiObjectId.trim().isEmpty()) {
            callback.onComplete(false, "测评缺少兴趣点信息");
            return;
        }

        review.delete(new UpdateListener() {
            @Override
            public void done(BmobException e) {
                if (e != null) {
                    callback.onComplete(false, "删除失败：" + e.getMessage());
                    return;
                }
                recalculatePoiScore(poiObjectId, callback);
            }
        });
    }

    private static void recalculatePoiScore(String poiObjectId, Callback callback) {
        CampusPoi poi = new CampusPoi();
        poi.setObjectId(poiObjectId);
        BmobQuery<Review> query = new BmobQuery<>();
        query.addWhereEqualTo("poi", poi);
        query.findObjects(new FindListener<Review>() {
            @Override
            public void done(List<Review> reviews, BmobException e) {
                if (e != null) {
                    callback.onComplete(true, "测评已删除，但评分统计更新失败");
                    return;
                }
                double total = 0;
                if (reviews != null) {
                    for (Review item : reviews) {
                        total += item.getScore() == null ? 0 : item.getScore();
                    }
                }
                int count = reviews == null ? 0 : reviews.size();
                CampusPoi update = new CampusPoi();
                update.setAvgScore(count == 0 ? 0 : total / count);
                update.setReviewCount(count);
                update.update(poiObjectId, new UpdateListener() {
                    @Override
                    public void done(BmobException updateError) {
                        callback.onComplete(
                                true,
                                updateError == null ? null : "测评已删除，但评分统计更新失败");
                    }
                });
            }
        });
    }
}

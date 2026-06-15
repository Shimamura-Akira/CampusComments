package com.example.campuscomments.util;

import android.util.Log;

import com.example.campuscomments.CampusUser;
import com.example.campuscomments.model.Review;
import com.example.campuscomments.model.ReviewFavorite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.CountListener;
import cn.bmob.v3.listener.UpdateListener;

public final class ReviewFavoriteUtils {
    private static final String TAG = "ReviewFavorite";
    private static final int ERROR_OBJECT_OR_CLASS_NOT_FOUND = 101;

    public interface LoadCallback {
        void onComplete(Set<String> reviewIds, String errorMessage);
    }

    public interface ToggleCallback {
        void onComplete(boolean favorite, int favoriteCount, String errorMessage);
    }

    private ReviewFavoriteUtils() {
    }

    public static void loadFavoriteReviewIds(CampusUser user, LoadCallback callback) {
        if (user == null) {
            callback.onComplete(new HashSet<>(), "请先登录");
            return;
        }
        BmobQuery<ReviewFavorite> query = new BmobQuery<>();
        query.addWhereEqualTo("user", user);
        query.include("review");
        query.setLimit(500);
        query.findObjects(new FindListener<ReviewFavorite>() {
            @Override
            public void done(List<ReviewFavorite> favorites, BmobException e) {
                if (e != null) {
                    Log.e(TAG, "Load favorites failed", e);
                    if (e.getErrorCode() == ERROR_OBJECT_OR_CLASS_NOT_FOUND) {
                        callback.onComplete(new HashSet<>(), null);
                    } else {
                        callback.onComplete(new HashSet<>(), formatError(e));
                    }
                    return;
                }
                Set<String> reviewIds = new HashSet<>();
                if (favorites != null) {
                    for (ReviewFavorite favorite : favorites) {
                        Review review = favorite.getReview();
                        if (review != null && review.getObjectId() != null) {
                            reviewIds.add(review.getObjectId());
                        }
                    }
                }
                callback.onComplete(reviewIds, null);
            }
        });
    }

    public static void toggleFavorite(CampusUser user, Review review, ToggleCallback callback) {
        if (user == null || review == null || review.getObjectId() == null) {
            callback.onComplete(false, 0, "无法收藏这条测评");
            return;
        }

        Review reviewRef = new Review();
        reviewRef.setObjectId(review.getObjectId());
        BmobQuery<ReviewFavorite> query = new BmobQuery<>();
        query.addWhereEqualTo("user", user);
        query.addWhereEqualTo("review", reviewRef);
        query.setLimit(20);
        query.findObjects(new FindListener<ReviewFavorite>() {
            @Override
            public void done(List<ReviewFavorite> favorites, BmobException e) {
                if (e != null) {
                    Log.e(TAG, "Query favorite state failed", e);
                    if (e.getErrorCode() == ERROR_OBJECT_OR_CLASS_NOT_FOUND) {
                        saveFavorite(user, review, reviewRef, callback);
                    } else {
                        callback.onComplete(false, safeCount(review), formatError(e));
                    }
                    return;
                }
                if (favorites != null && !favorites.isEmpty()) {
                    favorites.get(0).delete(new UpdateListener() {
                        @Override
                        public void done(BmobException deleteError) {
                            if (deleteError != null) {
                                callback.onComplete(
                                        true,
                                        safeCount(review),
                                        formatError(deleteError));
                            } else {
                                syncFavoriteCount(review, reviewRef, false, callback);
                            }
                        }
                    });
                    return;
                }

                saveFavorite(user, review, reviewRef, callback);
            }
        });
    }

    private static void saveFavorite(CampusUser user,
                                     Review review,
                                     Review reviewRef,
                                     ToggleCallback callback) {
        ReviewFavorite favorite = new ReviewFavorite();
        favorite.setUser(user);
        favorite.setReview(reviewRef);
        favorite.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                if (e != null) {
                    Log.e(TAG, "Save favorite failed", e);
                    callback.onComplete(false, safeCount(review), formatError(e));
                } else {
                    syncFavoriteCount(review, reviewRef, true, callback);
                }
            }
        });
    }

    private static void syncFavoriteCount(Review review,
                                          Review reviewRef,
                                          boolean favorite,
                                          ToggleCallback callback) {
        BmobQuery<ReviewFavorite> countQuery = new BmobQuery<>();
        countQuery.addWhereEqualTo("review", reviewRef);
        countQuery.count(ReviewFavorite.class, new CountListener() {
            @Override
            public void done(Integer count, BmobException e) {
                if (e != null) {
                    Log.e(TAG, "Count favorites failed", e);
                    int fallback = Math.max(
                            0,
                            safeCount(review) + (favorite ? 1 : -1));
                    review.setFavoriteCount(fallback);
                    callback.onComplete(favorite, fallback, null);
                    return;
                }

                int exactCount = count == null ? 0 : Math.max(0, count);
                Review update = new Review();
                update.setFavoriteCount(exactCount);
                update.update(review.getObjectId(), new UpdateListener() {
                    @Override
                    public void done(BmobException updateError) {
                        if (updateError != null) {
                            Log.e(TAG, "Update favorite count failed", updateError);
                        }
                        review.setFavoriteCount(exactCount);
                        callback.onComplete(favorite, exactCount, null);
                    }
                });
            }
        });
    }

    private static int safeCount(Review review) {
        return review == null || review.getFavoriteCount() == null
                ? 0
                : Math.max(0, review.getFavoriteCount());
    }

    private static String formatError(BmobException e) {
        return "Bmob " + e.getErrorCode() + "：" + e.getMessage();
    }
}

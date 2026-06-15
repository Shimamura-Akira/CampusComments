package com.example.campuscomments.model;

import com.example.campuscomments.CampusUser;

import cn.bmob.v3.BmobObject;

public class ReviewFavorite extends BmobObject {
    private CampusUser user;
    private Review review;

    public CampusUser getUser() {
        return user;
    }

    public void setUser(CampusUser user) {
        this.user = user;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }
}

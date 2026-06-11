package com.example.campuscomments.model;

import com.example.campuscomments.CampusUser;

import cn.bmob.v3.BmobObject;

public class Favorite extends BmobObject {
    private CampusUser user;
    private CampusPoi poi;

    public CampusUser getUser() {
        return user;
    }

    public void setUser(CampusUser user) {
        this.user = user;
    }

    public CampusPoi getPoi() {
        return poi;
    }

    public void setPoi(CampusPoi poi) {
        this.poi = poi;
    }
}

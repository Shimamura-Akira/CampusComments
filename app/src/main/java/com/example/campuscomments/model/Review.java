package com.example.campuscomments.model;

import com.example.campuscomments.CampusUser;

import cn.bmob.v3.BmobObject;

public class Review extends BmobObject {
    private CampusPoi poi;
    private CampusUser author;
    private Integer score;
    private String content;
    private String tags;

    public CampusPoi getPoi() {
        return poi;
    }

    public void setPoi(CampusPoi poi) {
        this.poi = poi;
    }

    public CampusUser getAuthor() {
        return author;
    }

    public void setAuthor(CampusUser author) {
        this.author = author;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}

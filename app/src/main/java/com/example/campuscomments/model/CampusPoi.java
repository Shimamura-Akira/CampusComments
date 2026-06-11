package com.example.campuscomments.model;

import com.example.campuscomments.CampusUser;

import cn.bmob.v3.BmobObject;

public class CampusPoi extends BmobObject {
    private String name;
    private String type;
    private Double latitude;
    private Double longitude;
    private String address;
    private String description;
    private String amapPoiId;
    private Double avgScore;
    private Integer reviewCount;
    private CampusUser createdBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAmapPoiId() {
        return amapPoiId;
    }

    public void setAmapPoiId(String amapPoiId) {
        this.amapPoiId = amapPoiId;
    }

    public Double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(Double avgScore) {
        this.avgScore = avgScore;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public CampusUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(CampusUser createdBy) {
        this.createdBy = createdBy;
    }
}

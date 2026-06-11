package com.example.campuscomments;

import cn.bmob.v3.BmobUser;

public class CampusUser extends BmobUser {
    private String nickname;
    private String school;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }
}

package com.nutsdev.socialslogintest;

import java.io.Serializable;

public class UserInfo implements Serializable {

    public String userName;
    public String userEmail;
    public String userId;
    public String userAvatarUrl;
    public String userProfileUrl;

    public UserInfo() {

    }

    public UserInfo(String userName, String userEmail, String userId, String userAvatarUrl) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userId = userId;
        this.userAvatarUrl = userAvatarUrl;
    }

}

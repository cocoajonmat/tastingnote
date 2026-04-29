package com.dongjin.tastingnote.user.entity;

public enum Provider {
    LOCAL("로컬"), KAKAO("카카오"), GOOGLE("구글"), NAVER("네이버");

    private String description;
    private Provider(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
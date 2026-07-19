package com.seoulection.admin.youtube.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class YoutubeUrlRegisterRequest {

    @NotBlank(message = "YouTube URL을 입력해 주세요.")
    @Size(max = 2_000, message = "URL은 2,000자 이하여야 합니다.")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

package com.seoulection.admin.youtube.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class YoutuberRegisterRequest {

    @NotBlank(message = "채널명을 입력해 주세요.")
    @Size(max = 200, message = "채널명은 200자 이하여야 합니다.")
    private String channelName;

    @NotBlank(message = "YouTube URL을 입력해 주세요.")
    @Size(max = 2_000, message = "URL은 2,000자 이하여야 합니다.")
    private String url;

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

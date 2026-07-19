package com.seoulection.admin.youtube.application.dto;

public record ParsedYoutubeChannelUrl(
        String channelName,
        String channelId,
        String url
) {
}

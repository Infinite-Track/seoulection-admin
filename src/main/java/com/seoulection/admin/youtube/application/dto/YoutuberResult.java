package com.seoulection.admin.youtube.application.dto;

import com.seoulection.admin.youtube.domain.entity.Youtuber;

import java.time.Instant;

public record YoutuberResult(
        String id,
        String channelName,
        String channelId,
        String url,
        Instant lastCheckedAt
) {
    public static YoutuberResult from(Youtuber youtuber) {
        return new YoutuberResult(
                youtuber.id(),
                youtuber.channelName(),
                youtuber.channelId(),
                youtuber.url(),
                youtuber.lastCheckedAt()
        );
    }
}

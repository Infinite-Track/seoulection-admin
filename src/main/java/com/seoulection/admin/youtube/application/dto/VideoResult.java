package com.seoulection.admin.youtube.application.dto;

import com.seoulection.admin.youtube.domain.entity.Video;
import com.seoulection.admin.youtube.domain.enums.VideoStatus;

public record VideoResult(
        String id,
        String youtuberId,
        String videoId,
        String title,
        String url,
        VideoStatus status
) {
    public static VideoResult from(Video video) {
        return new VideoResult(
                video.id(),
                video.youtuberId(),
                video.videoId(),
                video.title(),
                video.url(),
                video.status()
        );
    }
}

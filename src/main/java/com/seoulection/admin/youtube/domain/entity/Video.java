package com.seoulection.admin.youtube.domain.entity;

import com.seoulection.admin.youtube.domain.enums.VideoStatus;

import java.util.Objects;

public class Video {

    private final String id;
    private final String youtuberId;
    private final String videoId;
    private final String title;
    private final String url;
    private final VideoStatus status;

    private Video(
            String id,
            String youtuberId,
            String videoId,
            String title,
            String url,
            VideoStatus status
    ) {
        this.id = id;
        this.youtuberId = youtuberId;
        this.videoId = Objects.requireNonNull(videoId);
        this.title = title;
        this.url = Objects.requireNonNull(url);
        this.status = Objects.requireNonNull(status);
    }

    public static Video pending(String videoId, String url) {
        return new Video(null, null, videoId, null, url, VideoStatus.PENDING);
    }

    public static Video restore(
            String id,
            String youtuberId,
            String videoId,
            String title,
            String url,
            VideoStatus status
    ) {
        return new Video(id, youtuberId, videoId, title, url, status);
    }

    public String id() {
        return id;
    }

    public String youtuberId() {
        return youtuberId;
    }

    public String videoId() {
        return videoId;
    }

    public String title() {
        return title;
    }

    public String url() {
        return url;
    }

    public VideoStatus status() {
        return status;
    }
}

package com.seoulection.admin.youtube.domain.entity;

import java.time.Instant;
import java.util.Objects;

public class Youtuber {

    private final String id;
    private final String channelName;
    private final String channelId;
    private final String url;
    private final Instant lastCheckedAt;

    private Youtuber(
            String id,
            String channelName,
            String channelId,
            String url,
            Instant lastCheckedAt
    ) {
        this.id = id;
        this.channelName = channelName;
        this.channelId = channelId;
        this.url = Objects.requireNonNull(url);
        this.lastCheckedAt = lastCheckedAt;
    }

    public static Youtuber create(String channelName, String channelId, String url) {
        return new Youtuber(url, channelName, channelId, url, null);
    }

    public static Youtuber restore(
            String id,
            String channelName,
            String channelId,
            String url,
            Instant lastCheckedAt
    ) {
        return new Youtuber(id, channelName, channelId, url, lastCheckedAt);
    }

    public String id() {
        return id;
    }

    public String channelName() {
        return channelName;
    }

    public String channelId() {
        return channelId;
    }

    public String url() {
        return url;
    }

    public Instant lastCheckedAt() {
        return lastCheckedAt;
    }
}

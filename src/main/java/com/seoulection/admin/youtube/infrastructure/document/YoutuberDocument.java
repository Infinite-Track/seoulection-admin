package com.seoulection.admin.youtube.infrastructure.document;

import com.seoulection.admin.youtube.domain.entity.Youtuber;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "youtubers")
public class YoutuberDocument {

    @Id
    private String id;

    @Field("channel_name")
    private String channelName;

    @Field("channel_id")
    private String channelId;

    private String url;

    @Field("last_checked_at")
    private Instant lastCheckedAt;

    protected YoutuberDocument() {
    }

    private YoutuberDocument(Youtuber youtuber) {
        this.id = youtuber.id();
        this.channelName = youtuber.channelName();
        this.channelId = youtuber.channelId();
        this.url = youtuber.url();
        this.lastCheckedAt = youtuber.lastCheckedAt();
    }

    public static YoutuberDocument fromDomain(Youtuber youtuber) {
        return new YoutuberDocument(youtuber);
    }

    public Youtuber toDomain() {
        return Youtuber.restore(id, channelName, channelId, url, lastCheckedAt);
    }
}

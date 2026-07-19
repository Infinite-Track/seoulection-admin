package com.seoulection.admin.youtube.infrastructure.document;

import com.seoulection.admin.youtube.domain.entity.Video;
import com.seoulection.admin.youtube.domain.enums.VideoStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "videos")
public class VideoDocument {

    @Id
    private String id;

    @Field(name = "youtuber_id", targetType = FieldType.OBJECT_ID)
    private String youtuberId;

    @Field("video_id")
    private String videoId;

    private String title;
    private String url;
    private VideoStatus status;

    protected VideoDocument() {
    }

    private VideoDocument(Video video) {
        this.id = video.id();
        this.youtuberId = video.youtuberId();
        this.videoId = video.videoId();
        this.title = video.title();
        this.url = video.url();
        this.status = video.status();
    }

    public static VideoDocument fromDomain(Video video) {
        return new VideoDocument(video);
    }

    public Video toDomain() {
        return Video.restore(id, youtuberId, videoId, title, url, status);
    }
}

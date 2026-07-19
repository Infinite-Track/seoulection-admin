package com.seoulection.admin.youtube.infrastructure.repository;

import com.seoulection.admin.youtube.domain.entity.Video;
import com.seoulection.admin.youtube.domain.repository.VideoRepository;
import com.seoulection.admin.youtube.domain.entity.Youtuber;
import com.seoulection.admin.youtube.domain.repository.YoutuberRepository;
import com.seoulection.admin.youtube.infrastructure.document.VideoDocument;
import com.seoulection.admin.youtube.infrastructure.document.YoutuberDocument;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class YoutubeRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8");

    @Autowired
    YoutuberRepository youtuberRepository;

    @Autowired
    VideoRepository videoRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), YoutuberDocument.class);
        mongoTemplate.remove(new Query(), VideoDocument.class);
    }

    @Test
    @DisplayName("채널 링크는 youtubers 스키마로 저장한다")
    void saveYoutuber() {
        youtuberRepository.insert(Youtuber.create(
                "@beauty",
                null,
                "https://www.youtube.com/@beauty"
        ));
        youtuberRepository.insert(Youtuber.create(
                "@skincare",
                null,
                "https://www.youtube.com/@skincare"
        ));

        Document stored = mongoTemplate.getCollection("youtubers")
                .find(eq("url", "https://www.youtube.com/@beauty"))
                .first();

        assertThat(stored).isNotNull();
        assertThat(stored.getString("channel_name")).isEqualTo("@beauty");
        assertThat(stored.get("channel_id")).isNull();
        assertThat(stored.get("last_checked_at")).isNull();
        assertThat(mongoTemplate.getCollection("youtubers").countDocuments()).isEqualTo(2);
    }

    @Test
    @DisplayName("영상 링크는 videos 스키마와 PENDING 상태로 저장한다")
    void saveVideo() {
        videoRepository.insert(Video.pending(
                "dQw4w9WgXcQ",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        ));

        Document stored = mongoTemplate.getCollection("videos")
                .find(eq("video_id", "dQw4w9WgXcQ"))
                .first();

        assertThat(stored).isNotNull();
        assertThat(stored.get("youtuber_id")).isNull();
        assertThat(stored.get("title")).isNull();
        assertThat(stored.getString("status")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("채널 URL과 video ID는 각각 중복 저장할 수 없다")
    void rejectDuplicates() {
        Youtuber youtuber = Youtuber.create(
                "@beauty",
                null,
                "https://www.youtube.com/@beauty"
        );
        youtuberRepository.insert(youtuber);
        assertThatThrownBy(() -> youtuberRepository.insert(youtuber))
                .isInstanceOf(DuplicateKeyException.class);

        Video video = Video.pending(
                "dQw4w9WgXcQ",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        );
        videoRepository.insert(video);
        assertThatThrownBy(() -> videoRepository.insert(video))
                .isInstanceOf(DuplicateKeyException.class);
    }
}

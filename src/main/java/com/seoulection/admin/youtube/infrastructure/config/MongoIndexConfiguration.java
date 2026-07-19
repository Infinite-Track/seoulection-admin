package com.seoulection.admin.youtube.infrastructure.config;

import com.seoulection.admin.youtube.infrastructure.document.VideoDocument;
import com.seoulection.admin.youtube.infrastructure.document.YoutuberDocument;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class MongoIndexConfiguration {

    @Bean
    ApplicationRunner youtubeIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            mongoTemplate.indexOps(YoutuberDocument.class).createIndex(
                    new Index()
                            .on("url", Sort.Direction.ASC)
                            .unique()
                            .named("uk_youtubers_url")
            );
            mongoTemplate.indexOps(VideoDocument.class).createIndex(
                    new Index()
                            .on("video_id", Sort.Direction.ASC)
                            .unique()
                            .named("uk_videos_video_id")
            );
        };
    }
}

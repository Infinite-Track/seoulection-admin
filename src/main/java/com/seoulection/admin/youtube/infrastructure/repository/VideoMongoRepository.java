package com.seoulection.admin.youtube.infrastructure.repository;

import com.seoulection.admin.youtube.infrastructure.document.VideoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

interface VideoMongoRepository extends MongoRepository<VideoDocument, String> {

    boolean existsByVideoId(String videoId);
}

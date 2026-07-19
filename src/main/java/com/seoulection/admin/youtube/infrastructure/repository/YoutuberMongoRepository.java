package com.seoulection.admin.youtube.infrastructure.repository;

import com.seoulection.admin.youtube.infrastructure.document.YoutuberDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

interface YoutuberMongoRepository extends MongoRepository<YoutuberDocument, String> {

    boolean existsByUrl(String url);

    List<YoutuberDocument> findAllByOrderByChannelNameAsc();
}

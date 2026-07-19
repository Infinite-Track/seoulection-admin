package com.seoulection.admin.youtube.infrastructure.repository;

import com.seoulection.admin.youtube.domain.entity.Video;
import com.seoulection.admin.youtube.domain.repository.VideoRepository;
import com.seoulection.admin.youtube.infrastructure.document.VideoDocument;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VideoRepositoryImpl implements VideoRepository {

    private final VideoMongoRepository mongoRepository;

    public VideoRepositoryImpl(VideoMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public boolean existsByVideoId(String videoId) {
        return mongoRepository.existsByVideoId(videoId);
    }

    @Override
    public Video insert(Video video) {
        return mongoRepository.insert(VideoDocument.fromDomain(video)).toDomain();
    }

    @Override
    public List<Video> findAll() {
        return mongoRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(VideoDocument::toDomain)
                .toList();
    }
}

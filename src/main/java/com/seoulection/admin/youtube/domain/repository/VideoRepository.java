package com.seoulection.admin.youtube.domain.repository;

import com.seoulection.admin.youtube.domain.entity.Video;

import java.util.List;

public interface VideoRepository {

    boolean existsByVideoId(String videoId);

    Video insert(Video video);

    List<Video> findAll();
}

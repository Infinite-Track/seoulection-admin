package com.seoulection.admin.youtube.domain.repository;

import com.seoulection.admin.youtube.domain.entity.Youtuber;

import java.util.List;
import java.util.Optional;

public interface YoutuberRepository {

    boolean existsByUrl(String url);

    Youtuber insert(Youtuber youtuber);

    Optional<Youtuber> findById(String id);

    List<Youtuber> findAllOrderByChannelName();
}

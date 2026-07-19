package com.seoulection.admin.youtube.infrastructure.repository;

import com.seoulection.admin.youtube.domain.entity.Youtuber;
import com.seoulection.admin.youtube.domain.repository.YoutuberRepository;
import com.seoulection.admin.youtube.infrastructure.document.YoutuberDocument;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class YoutuberRepositoryImpl implements YoutuberRepository {

    private final YoutuberMongoRepository mongoRepository;

    public YoutuberRepositoryImpl(YoutuberMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public boolean existsByUrl(String url) {
        return mongoRepository.existsByUrl(url);
    }

    @Override
    public Youtuber insert(Youtuber youtuber) {
        return mongoRepository.insert(YoutuberDocument.fromDomain(youtuber)).toDomain();
    }

    @Override
    public Optional<Youtuber> findById(String id) {
        return mongoRepository.findById(id).map(YoutuberDocument::toDomain);
    }

    @Override
    public List<Youtuber> findAllOrderByChannelName() {
        return mongoRepository.findAllByOrderByChannelNameAsc()
                .stream()
                .map(YoutuberDocument::toDomain)
                .toList();
    }
}

package com.seoulection.admin.youtube.application.service;

import com.seoulection.admin.youtube.application.dto.YoutuberResult;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import com.seoulection.admin.youtube.domain.entity.Youtuber;
import com.seoulection.admin.youtube.domain.repository.YoutuberRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YoutuberService {

    private final YoutuberRepository repository;

    public YoutuberService(YoutuberRepository repository) {
        this.repository = repository;
    }

    public YoutuberResult register(String channelName, String url) {
        if (repository.existsByUrl(url)) {
            throw duplicate(url, null);
        }

        try {
            return YoutuberResult.from(repository.insert(Youtuber.create(
                    channelName,
                    null,
                    url
            )));
        } catch (DuplicateKeyException e) {
            throw duplicate(url, e);
        }
    }

    public List<YoutuberResult> getYoutubers() {
        return repository.findAllOrderByChannelName()
                .stream()
                .map(YoutuberResult::from)
                .toList();
    }

    private YoutubeAdminException duplicate(String url, Throwable cause) {
        String detail = "이미 등록된 YouTube 채널 url=" + url;
        return cause == null
                ? new YoutubeAdminException(YoutubeAdminException.Reason.DUPLICATE_CHANNEL, detail)
                : new YoutubeAdminException(
                YoutubeAdminException.Reason.DUPLICATE_CHANNEL,
                detail,
                cause
        );
    }
}

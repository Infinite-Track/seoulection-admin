package com.seoulection.admin.youtube.application.service;

import com.seoulection.admin.youtube.application.dto.ParsedYoutubeChannelUrl;
import com.seoulection.admin.youtube.application.dto.YoutuberResult;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import com.seoulection.admin.youtube.domain.entity.Youtuber;
import com.seoulection.admin.youtube.domain.repository.YoutuberRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YoutuberService {

    private final YoutubeChannelUrlParser urlParser;
    private final YoutuberRepository repository;

    public YoutuberService(YoutubeChannelUrlParser urlParser, YoutuberRepository repository) {
        this.urlParser = urlParser;
        this.repository = repository;
    }

    public YoutuberResult register(String rawUrl) {
        ParsedYoutubeChannelUrl parsed = urlParser.parse(rawUrl);
        if (repository.existsByUrl(parsed.url())) {
            throw duplicate(parsed.url(), null);
        }

        try {
            return YoutuberResult.from(repository.insert(Youtuber.create(
                    parsed.channelName(),
                    parsed.channelId(),
                    parsed.url()
            )));
        } catch (DuplicateKeyException e) {
            throw duplicate(parsed.url(), e);
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

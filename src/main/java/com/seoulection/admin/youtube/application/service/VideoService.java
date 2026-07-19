package com.seoulection.admin.youtube.application.service;

import com.seoulection.admin.youtube.application.dto.ParsedYoutubeUrl;
import com.seoulection.admin.youtube.application.dto.VideoResult;
import com.seoulection.admin.youtube.domain.entity.Video;
import com.seoulection.admin.youtube.domain.repository.VideoRepository;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoService {

    private final YoutubeUrlParser urlParser;
    private final VideoRepository repository;

    public VideoService(YoutubeUrlParser urlParser, VideoRepository repository) {
        this.urlParser = urlParser;
        this.repository = repository;
    }

    public VideoResult register(String rawUrl) {
        ParsedYoutubeUrl parsed = urlParser.parse(rawUrl);
        if (repository.existsByVideoId(parsed.videoId())) {
            throw duplicate(parsed.videoId(), null);
        }

        try {
            return VideoResult.from(repository.insert(Video.pending(
                    parsed.videoId(),
                    parsed.canonicalUrl()
            )));
        } catch (DuplicateKeyException e) {
            throw duplicate(parsed.videoId(), e);
        }
    }

    public List<VideoResult> getVideos() {
        return repository.findAll()
                .stream()
                .map(VideoResult::from)
                .toList();
    }

    private YoutubeAdminException duplicate(String videoId, Throwable cause) {
        String detail = "이미 등록된 YouTube 영상 videoId=" + videoId;
        return cause == null
                ? new YoutubeAdminException(YoutubeAdminException.Reason.DUPLICATE_VIDEO, detail)
                : new YoutubeAdminException(
                YoutubeAdminException.Reason.DUPLICATE_VIDEO,
                detail,
                cause
        );
    }
}

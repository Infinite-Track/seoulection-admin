package com.seoulection.admin.youtube.application;

import com.seoulection.admin.youtube.application.dto.ParsedYoutubeUrl;
import com.seoulection.admin.youtube.application.dto.VideoResult;
import com.seoulection.admin.youtube.application.service.VideoService;
import com.seoulection.admin.youtube.application.service.YoutubeUrlParser;
import com.seoulection.admin.youtube.domain.entity.Video;
import com.seoulection.admin.youtube.domain.repository.VideoRepository;
import com.seoulection.admin.youtube.domain.enums.VideoStatus;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    YoutubeUrlParser parser;

    @Mock
    VideoRepository repository;

    @InjectMocks
    VideoService service;

    @Test
    @DisplayName("영상 링크를 PENDING 영상으로 등록한다")
    void register() {
        ParsedYoutubeUrl parsed = new ParsedYoutubeUrl(
                "dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        );
        given(parser.parse(parsed.originalUrl())).willReturn(parsed);
        given(repository.existsByVideoId(parsed.videoId())).willReturn(false);
        given(repository.insert(org.mockito.ArgumentMatchers.any(Video.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        VideoResult result = service.register(parsed.originalUrl());

        assertThat(result.videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(result.status()).isEqualTo(VideoStatus.PENDING);
        assertThat(result.title()).isNull();
        assertThat(result.youtuberId()).isNull();
    }

    @Test
    @DisplayName("같은 video ID는 중복 등록하지 않는다")
    void rejectDuplicate() {
        ParsedYoutubeUrl parsed = new ParsedYoutubeUrl(
                "dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        );
        given(parser.parse(parsed.originalUrl())).willReturn(parsed);
        given(repository.existsByVideoId(parsed.videoId())).willReturn(true);

        assertThatThrownBy(() -> service.register(parsed.originalUrl()))
                .isInstanceOf(YoutubeAdminException.class)
                .extracting(e -> ((YoutubeAdminException) e).reason())
                .isEqualTo(YoutubeAdminException.Reason.DUPLICATE_VIDEO);
    }
}

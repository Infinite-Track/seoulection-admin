package com.seoulection.admin.youtube.application;

import com.seoulection.admin.youtube.application.dto.ParsedYoutubeUrl;
import com.seoulection.admin.youtube.application.service.YoutubeUrlParser;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YoutubeUrlParserTest {

    private final YoutubeUrlParser parser = new YoutubeUrlParser();

    @Test
    @DisplayName("watch URL에서 video ID를 추출하고 URL을 정규화한다")
    void parseWatchUrl() {
        ParsedYoutubeUrl parsed = parser.parse(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=test"
        );

        assertThat(parsed.videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(parsed.canonicalUrl())
                .isEqualTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("youtu.be와 shorts URL도 같은 방식으로 처리한다")
    void parseAlternativeUrls() {
        assertThat(parser.parse("youtu.be/dQw4w9WgXcQ?t=10").videoId())
                .isEqualTo("dQw4w9WgXcQ");
        assertThat(parser.parse("https://youtube.com/shorts/dQw4w9WgXcQ").videoId())
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("YouTube가 아닌 URL은 거절한다")
    void rejectNonYoutubeUrl() {
        assertThatThrownBy(() -> parser.parse("https://example.com/watch?v=dQw4w9WgXcQ"))
                .isInstanceOf(YoutubeAdminException.class)
                .extracting(e -> ((YoutubeAdminException) e).reason())
                .isEqualTo(YoutubeAdminException.Reason.INVALID_VIDEO_URL);
    }
}

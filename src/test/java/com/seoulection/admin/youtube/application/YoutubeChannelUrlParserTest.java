package com.seoulection.admin.youtube.application;

import com.seoulection.admin.youtube.application.dto.ParsedYoutubeChannelUrl;
import com.seoulection.admin.youtube.application.service.YoutubeChannelUrlParser;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YoutubeChannelUrlParserTest {

    private final YoutubeChannelUrlParser parser = new YoutubeChannelUrlParser();

    @Test
    @DisplayName("핸들 기반 채널 링크를 정규화한다")
    void parseHandleUrl() {
        ParsedYoutubeChannelUrl parsed = parser.parse("youtube.com/@beauty-channel/videos");

        assertThat(parsed.channelName()).isEqualTo("@beauty-channel");
        assertThat(parsed.channelId()).isNull();
        assertThat(parsed.url()).isEqualTo("https://www.youtube.com/@beauty-channel");
    }

    @Test
    @DisplayName("채널 ID 기반 링크에서 channel ID를 추출한다")
    void parseChannelIdUrl() {
        ParsedYoutubeChannelUrl parsed = parser.parse(
                "https://www.youtube.com/channel/UC_x5XG1OV2P6uZZ5FSM9Ttw"
        );

        assertThat(parsed.channelId()).isEqualTo("UC_x5XG1OV2P6uZZ5FSM9Ttw");
        assertThat(parsed.channelName()).isNull();
    }

    @Test
    @DisplayName("채널이 아닌 YouTube 링크는 거절한다")
    void rejectVideoUrl() {
        assertThatThrownBy(() -> parser.parse(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        ))
                .isInstanceOf(YoutubeAdminException.class)
                .extracting(e -> ((YoutubeAdminException) e).reason())
                .isEqualTo(YoutubeAdminException.Reason.INVALID_CHANNEL);
    }
}

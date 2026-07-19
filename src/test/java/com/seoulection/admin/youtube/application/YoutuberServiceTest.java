package com.seoulection.admin.youtube.application;

import com.seoulection.admin.youtube.application.dto.ParsedYoutubeChannelUrl;
import com.seoulection.admin.youtube.application.dto.YoutuberResult;
import com.seoulection.admin.youtube.application.service.YoutubeChannelUrlParser;
import com.seoulection.admin.youtube.application.service.YoutuberService;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import com.seoulection.admin.youtube.domain.entity.Youtuber;
import com.seoulection.admin.youtube.domain.repository.YoutuberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class YoutuberServiceTest {

    @Mock
    YoutubeChannelUrlParser parser;

    @Mock
    YoutuberRepository repository;

    @InjectMocks
    YoutuberService service;

    @Test
    @DisplayName("채널 링크를 유튜버 컬렉션에 등록한다")
    void register() {
        ParsedYoutubeChannelUrl parsed = new ParsedYoutubeChannelUrl(
                "@beauty",
                null,
                "https://www.youtube.com/@beauty"
        );
        given(parser.parse(parsed.url())).willReturn(parsed);
        given(repository.existsByUrl(parsed.url())).willReturn(false);
        given(repository.insert(org.mockito.ArgumentMatchers.any(Youtuber.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        YoutuberResult result = service.register(parsed.url());

        assertThat(result.channelName()).isEqualTo("@beauty");
        assertThat(result.lastCheckedAt()).isNull();
        then(repository).should().insert(org.mockito.ArgumentMatchers.any(Youtuber.class));
    }

    @Test
    @DisplayName("같은 정규화 채널 URL은 중복 등록하지 않는다")
    void rejectDuplicate() {
        ParsedYoutubeChannelUrl parsed = new ParsedYoutubeChannelUrl(
                "@beauty",
                null,
                "https://www.youtube.com/@beauty"
        );
        given(parser.parse(parsed.url())).willReturn(parsed);
        given(repository.existsByUrl(parsed.url())).willReturn(true);

        assertThatThrownBy(() -> service.register(parsed.url()))
                .isInstanceOf(YoutubeAdminException.class)
                .extracting(e -> ((YoutubeAdminException) e).reason())
                .isEqualTo(YoutubeAdminException.Reason.DUPLICATE_CHANNEL);
    }

}

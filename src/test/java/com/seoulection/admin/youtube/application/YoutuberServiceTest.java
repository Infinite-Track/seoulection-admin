package com.seoulection.admin.youtube.application;

import com.seoulection.admin.youtube.application.dto.YoutuberResult;
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
    YoutuberRepository repository;

    @InjectMocks
    YoutuberService service;

    @Test
    @DisplayName("채널 링크를 유튜버 컬렉션에 등록한다")
    void register() {
        String url = "https://www.youtube.com/@beauty/videos?view=0";
        given(repository.existsByUrl(url)).willReturn(false);
        given(repository.insert(org.mockito.ArgumentMatchers.any(Youtuber.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        YoutuberResult result = service.register("뷰티 채널", url);

        assertThat(result.id()).isEqualTo(url);
        assertThat(result.channelName()).isEqualTo("뷰티 채널");
        assertThat(result.url()).isEqualTo(url);
        assertThat(result.channelId()).isNull();
        assertThat(result.lastCheckedAt()).isNull();
        then(repository).should().insert(org.mockito.ArgumentMatchers.any(Youtuber.class));
    }

    @Test
    @DisplayName("같은 정규화 채널 URL은 중복 등록하지 않는다")
    void rejectDuplicate() {
        String url = "https://www.youtube.com/@beauty";
        given(repository.existsByUrl(url)).willReturn(true);

        assertThatThrownBy(() -> service.register("뷰티 채널", url))
                .isInstanceOf(YoutubeAdminException.class)
                .extracting(e -> ((YoutubeAdminException) e).reason())
                .isEqualTo(YoutubeAdminException.Reason.DUPLICATE_CHANNEL);
    }

}

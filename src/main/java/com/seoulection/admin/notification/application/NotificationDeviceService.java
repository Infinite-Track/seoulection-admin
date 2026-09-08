package com.seoulection.admin.notification.application;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 기기 등록 유스케이스.
 *
 * <p>지금은 포트로 넘기기만 한다. 그래도 두는 이유는 <b>규칙을 놓을 자리</b>가 필요해서다 —
 * 사용자 ID 검증, "이미 해제된 기기" 판단, 해제 사유 기록 같은 것이 생기면 화면도 어댑터도
 * 아닌 여기가 그 자리다. {@code ProductService}·{@code SurveyService} 와 같은 층이다.
 *
 * <p>컨트롤러가 포트를 직접 잡으면 그 규칙이 화면으로 새거나 어댑터마다 중복된다.
 */
@Service
public class NotificationDeviceService {

    private final NotificationDevicePort devicePort;

    public NotificationDeviceService(NotificationDevicePort devicePort) {
        this.devicePort = devicePort;
    }

    /** 사용자 ID 가 없으면 조회하지 않는다 — 전체 기기 목록은 어드민이 볼 이유가 없고 양도 많다. */
    public List<NotificationDevicePort.DeviceView> activeDevices(Long userId) {
        return userId == null ? List.of() : devicePort.activeOf(userId);
    }

    public void revoke(Long registrationId) {
        if (registrationId == null) {
            throw new IllegalArgumentException("해제할 기기를 지정해 주세요.");
        }
        devicePort.revoke(registrationId);
    }
}

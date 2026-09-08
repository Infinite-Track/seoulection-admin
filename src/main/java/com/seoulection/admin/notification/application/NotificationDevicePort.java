package com.seoulection.admin.notification.application;

import java.util.List;

/**
 * 기기 등록 접근 <b>포트</b>.
 *
 * <p>{@code ProductIngredientPort} 와 같은 이유로 인터페이스다 — 지금은 notification-service 의
 * 어드민 API 를 부르지만, 저장소가 옮겨 가거나 서비스가 분리돼도 화면은 이 계약만 본다.
 */
public interface NotificationDevicePort {

    List<DeviceView> activeOf(Long userId);

    /** 지우지 않고 해제한다 — 서비스 쪽이 revoked_at 을 찍는다. */
    void revoke(Long registrationId);

    /** 토큰 전문은 받지 않는다. 화면은 기기를 구분하기만 하면 된다. */
    record DeviceView(Long id, String platform, String tokenPreview) {}
}

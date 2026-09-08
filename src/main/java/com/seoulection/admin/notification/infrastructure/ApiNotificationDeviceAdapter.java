package com.seoulection.admin.notification.infrastructure;

import com.seoulection.admin.notification.application.NotificationDevicePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * notification-service 의 어드민 API 를 부른다.
 *
 * <p>🔴 서비스 키가 비어 있으면 403 이 나는데, 그 실패는 기동이 아니라 화면을 눌렀을 때
 * 드러난다. {@code admin.notification-service.service-key} 를 반드시 함께 넣을 것.
 */
@Component
public class ApiNotificationDeviceAdapter implements NotificationDevicePort {

    private final RestClient client;
    private final String serviceKey;

    public ApiNotificationDeviceAdapter(
            @Value("${admin.notification-service.base-url:http://notification-service:8080}") String baseUrl,
            @Value("${admin.notification-service.service-key:}") String serviceKey) {
        this.serviceKey = serviceKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public List<DeviceView> activeOf(Long userId) {
        List<DeviceView> devices = client.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/admin/v1/notifications/devices")
                        .queryParam("userId", userId).build())
                .header("X-Service-Key", serviceKey)
                .retrieve().body(new ParameterizedTypeReference<List<DeviceView>>() {});
        return devices == null ? List.of() : devices;
    }

    @Override
    public void revoke(Long registrationId) {
        client.delete().uri("/internal/admin/v1/notifications/devices/{id}", registrationId)
                .header("X-Service-Key", serviceKey)
                .retrieve().toBodilessEntity();
    }
}

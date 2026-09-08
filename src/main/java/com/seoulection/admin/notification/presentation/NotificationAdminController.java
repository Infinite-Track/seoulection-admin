package com.seoulection.admin.notification.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 수동 푸시 발송 화면의 진입점.
 *
 * <p>아직 notification-service와 연결하지 않았으므로 이 컨트롤러는 데이터를 만들거나 발송하지 않는다.
 * 다음 단계에서 이 패키지에 notification-service HTTP 클라이언트를 붙이면, 템플릿의 대상 수와 이력
 * 영역만 실제 응답으로 교체하면 된다.</p>
 */
@Controller
public class NotificationAdminController {

    @GetMapping("/admin/notifications")
    public String page() {
        return "notifications";
    }
}

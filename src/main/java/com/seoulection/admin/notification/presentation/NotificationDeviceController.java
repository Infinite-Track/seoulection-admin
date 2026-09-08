package com.seoulection.admin.notification.presentation;

import com.seoulection.admin.notification.application.NotificationDeviceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * 기기 토큰 관리 화면.
 *
 * <p>문의 대응용이다 — "푸시가 안 온다", "지운 기기로 계속 온다" 는 죽은 토큰이 원인인 경우가
 * 많은데, 그것을 확인하고 끊을 자리가 없었다.
 *
 * <p>사용자 ID 로만 조회한다. 토큰으로 역조회하지 않는 이유: 어드민이 토큰을 알 방법이 없고,
 * 토큰을 입력받는 화면을 두면 그 값이 로그·브라우저 기록에 남는다.
 */
@Controller
public class NotificationDeviceController {

    private final NotificationDeviceService service;

    public NotificationDeviceController(NotificationDeviceService service) {
        this.service = service;
    }

    @GetMapping("/admin/notifications/devices")
    public String page(@RequestParam(required = false) Long userId, Model model) {
        model.addAttribute("userId", userId);
        model.addAttribute("devices", service.activeDevices(userId));
        model.addAttribute("searched", userId != null);
        return "notification-devices";
    }

    @PostMapping("/admin/notifications/devices/{registrationId}/revoke")
    public String revoke(@PathVariable Long registrationId, @RequestParam Long userId,
                         RedirectAttributes redirectAttributes) {
        service.revoke(registrationId);
        redirectAttributes.addFlashAttribute("successMessage",
                "기기 등록을 해제했습니다. 이 기기로는 더 이상 발송되지 않습니다.");
        return "redirect:/admin/notifications/devices?userId=" + userId;
    }
}

package com.chatji.chatji.controller;

import com.chatji.chatji.domain.alert.Alert;
import com.chatji.chatji.domain.alert.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertService alertService; // v30.1: 알림 서비스 주입

    /**
     * 알림 설정 등록 및 즉시 검사 실행
     */
    @PostMapping
    public Alert createAlert(@RequestBody Alert alert) {
        alertService.addAlertAndCheckImmediately(alert);
        return alert;
    }

    /**
     * 유저별 등록된 알림 리스트 조회
     */
    @GetMapping("/{userId}")
    public List<Alert> getMyAlerts(@PathVariable String userId) {
        return alertRepository.findByUserId(userId);
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{alertId}")
    public void deleteAlert(@PathVariable Long alertId) {
        alertRepository.deleteById(alertId);
    }
}

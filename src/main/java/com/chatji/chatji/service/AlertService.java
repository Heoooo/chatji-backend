package com.chatji.chatji.service;

import com.chatji.chatji.controller.NotificationController;
import com.chatji.chatji.domain.alert.Alert;
import com.chatji.chatji.domain.alert.AlertRepository;
import com.chatji.chatji.domain.hotdeal.HotDeal;
import com.chatji.chatji.domain.hotdeal.HotDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final HotDealRepository hotDealRepository; // v30.1: 즉시 검사용 레포지토리 추가

    /**
     * v30: 모든 활성화된 키워드 알림 체크 (새로운 핫딜 유입 시)
     */
    public void checkKeywordAlerts(HotDeal deal) {
        List<Alert> keywordAlerts = alertRepository.findAllByActiveTrue().stream()
                .filter(a -> a.getType() == Alert.AlertType.KEYWORD)
                .toList();

        for (Alert alert : keywordAlerts) {
            matchAndSend(alert, deal);
        }
    }

    /**
     * v30.1: 알림 등록 시 기존 핫딜 데이터 즉시 전수 조사!
     */
    public void addAlertAndCheckImmediately(Alert alert) {
        alertRepository.save(alert);
        
        if (alert.getType() == Alert.AlertType.KEYWORD) {
            log.info("[IMMEDIATE-CHECK] Scanning existing deals for keyword: {}", alert.getKeyword());
            List<HotDeal> existingDeals = hotDealRepository.findAll();
            for (HotDeal deal : existingDeals) {
                matchAndSend(alert, deal);
            }
        }
    }

    private void matchAndSend(Alert alert, HotDeal deal) {
        if (deal.getTitle().toLowerCase().contains(alert.getKeyword().toLowerCase())) {
            log.info("[KEYWORD-MATCH] User: {}, Keyword: {}", alert.getUserId(), alert.getKeyword());
            NotificationController.sendToUser(alert.getUserId(), "keyword_match", deal);
        }
    }

    /**
     * v30: 특정 상품의 목표가 도달 여부 체크 (시세 변동 시)
     */
    public void checkTargetPriceAlerts(String productId, int currentPrice, String title, String url) {
        List<Alert> priceAlerts = alertRepository.findAllByActiveTrue().stream()
                .filter(a -> a.getType() == Alert.AlertType.TARGET_PRICE && productId.equals(a.getProductId()))
                .toList();

        for (Alert alert : priceAlerts) {
            if (currentPrice <= alert.getTargetPrice()) {
                log.info("[PRICE-MATCH] User: {}, Target: {}", alert.getUserId(), alert.getTargetPrice());
                NotificationController.sendToUser(alert.getUserId(), "price_match", title + " 목표가 도달! (" + currentPrice + "원)");
            }
        }
    }
}

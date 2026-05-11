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
     * v31: 알림 매칭 엔진 최적화 (O(N) Loop -> DB Query)
     */
    public void checkNewHotDealAlerts(HotDeal deal) {
        long startTime = System.nanoTime();

        // 1. 기존 방식 (비교용 주석 처리 또는 로그 기록 가능)
        // List<Alert> all = alertRepository.findAllByActiveTrue(); // O(N)
        
        // 2. 최적화된 방식: DB 인덱스를 활용한 필터링
        List<Alert> matchedAlerts = alertRepository.findMatchingAlerts(
                deal.getTitle(), deal.getCategoryLarge(), deal.getCategorySmall()
        );

        for (Alert alert : matchedAlerts) {
            NotificationController.sendToUser(alert.getUserId(), "hotdeal_match", deal);
        }

        long endTime = System.nanoTime();
        log.info("[v31-OPTIMIZATION] Matching completed for deal: {}. Matched: {} users. Time: {} ms", 
                deal.getTitle(), matchedAlerts.size(), (endTime - startTime) / 1_000_000.0);
    }

    /**
     * v30.1: 알림 등록 시 기존 핫딜 데이터 즉시 전수 조사! (카테고리 포함)
     * TODO: 이 부분도 HotDeal 테이블에 대한 인덱스 쿼리로 최적화 가능
     */
    public void addAlertAndCheckImmediately(Alert alert) {
        alertRepository.save(alert);
        
        log.info("[IMMEDIATE-CHECK] Scanning existing deals for new alert: {}", alert.getType());
        List<HotDeal> existingDeals = hotDealRepository.findAll();
        for (HotDeal deal : existingDeals) {
            matchAndSend(alert, deal);
        }
    }

    private void matchAndSend(Alert alert, HotDeal deal) {
        boolean isMatch = false;
        // (기본 매칭 로직 유지 - addAlertAndCheckImmediately에서 사용 중)

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

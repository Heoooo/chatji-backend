package com.chatji.chatji.service;

import com.chatji.chatji.domain.hotdeal.HotDeal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlarmService {

    /**
     * 포트폴리오 포인트: "이벤트 기반 알림 처리"
     * 향후 Slack, Telegram, Kakao 등으로 쉽게 확장 가능한 구조
     */
    public void sendHotDealAlarm(HotDeal deal, int discountRate) {
        String message = String.format(
            "🔥 [역대급 핫딜 포착!] 🔥\n" +
            "📢 상품명: %s\n" +
            "💰 쇼핑몰: %s\n" +
            "📉 네이버 최저가 대비 %d%% 저렴!\n" +
            "🔗 링크: %s",
            deal.getTitle(), deal.getMallName(), discountRate, deal.getUrl()
        );

        // 실서버 배포 시 여기에 Webhook URL 호출 로직 추가 (Rest Client 사용)
        log.info("[ALARM-TRIGGERED]\n{}", message);
    }
}

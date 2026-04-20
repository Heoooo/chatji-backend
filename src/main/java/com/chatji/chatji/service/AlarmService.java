package com.chatji.chatji.service;

import com.chatji.chatji.controller.NotificationController;
import com.chatji.chatji.domain.hotdeal.HotDeal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlarmService {

    /**
     * v29: 핫딜 발생 시 실제 컴퓨터 알람 송신 (SSE 브로드캐스트)
     */
    public void sendHotDealAlarm(HotDeal deal, int discountRate) {
        String message = String.format(
            "🔥 [역대급 핫딜 포착!] 🔥\n" +
            "📢 상품명: %s\n" +
            "📉 할인율: %d%%\n",
            deal.getTitle(), discountRate
        );

        log.info("[ALARM-TRIGGERED]\n{}", message);

        // v29: 실시간 채널로 전송 (브라우저 푸시용)
        NotificationController.broadcast("hotdeal", deal);
    }
}

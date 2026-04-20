package com.chatji.chatji.domain.alert;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId; // v30: 알림을 받을 유저 식별자

    @Enumerated(EnumType.STRING)
    private AlertType type; // KEYWORD, TARGET_PRICE

    private String keyword; // 키워드 알림용
    private String productId; // 목표가 알림용 (특정 상품)
    private Integer targetPrice; // 목표 가격

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AlertType {
        KEYWORD, TARGET_PRICE
    }
}

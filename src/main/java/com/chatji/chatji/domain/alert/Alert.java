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
@Table(name = "alert", indexes = {
        @Index(name = "idx_alert_keyword", columnList = "keyword"),
        @Index(name = "idx_alert_category", columnList = "categoryLarge, categorySmall"),
        @Index(name = "idx_alert_active", columnList = "active")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId; // v30: 알림을 받을 유저 식별자

    @Enumerated(EnumType.STRING)
    private AlertType type; // KEYWORD, TARGET_PRICE, CATEGORY

    private String keyword; // 키워드 알림용
    private String categoryLarge; // 대분류 (예: 가전, 식품)
    private String categorySmall; // 소분류 (예: 노트북, 생수)
    private String productId; // 목표가 알림용 (특정 상품)
    private Integer targetPrice; // 목표 가격

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AlertType {
        KEYWORD, TARGET_PRICE, CATEGORY
    }
}

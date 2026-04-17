package com.chatji.chatji.domain.hotdeal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HotDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String originId; // 원문 게시글 ID (중복 수집 방지)

    private String title;
    private String url;
    private Integer currentPrice;
    private String mallName;
    private String source; // 뿜뿌, 루리웹 등 출처
    private Integer score; // v27: 핫딜 점수 (0-100)
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private boolean isNotified = false; // 알림 발송 여부

    public void markAsNotified() {
        this.isNotified = true;
    }
}

package com.chatji.chatji.domain.price;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // 상품 구분을 위한 키워드 보조 저장
    private String keyword;
}

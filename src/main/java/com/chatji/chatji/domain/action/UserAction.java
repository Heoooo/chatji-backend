package com.chatji.chatji.domain.action;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 비회원이어도 브라우저 ID(UUID) 등을 통해 추Tracking 가능하게 설계
    private String userId; 

    private String category; // 클릭한 상품의 카테고리 정보

    private String actionType; // CLICK, WISH, etc.

    private LocalDateTime createdAt;
}

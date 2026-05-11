package com.chatji.chatji.domain.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByActiveTrue();
    List<Alert> findByUserId(String userId);

    /**
     * v31: 핫딜 정보를 기반으로 매칭되는 모든 알림을 DB 레벨에서 한 번에 조회
     */
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Alert a WHERE a.active = true AND (" +
            "(a.type = 'KEYWORD' AND :title LIKE CONCAT('%', a.keyword, '%')) OR " +
            "(a.type = 'CATEGORY' AND (a.categorySmall = :catSmall OR (a.categorySmall IS NULL AND a.categoryLarge = :catLarge)))" +
            ")")
    List<Alert> findMatchingAlerts(String title, String catLarge, String catSmall);
}

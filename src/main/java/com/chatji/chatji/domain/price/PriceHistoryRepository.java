package com.chatji.chatji.domain.price;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductIdOrderByTimestampAsc(String productId);
    
    // 특정 키워드에 대한 최근 시세 추이를 보기 위함
    List<PriceHistory> findTop20ByKeywordOrderByTimestampDesc(String keyword);
}

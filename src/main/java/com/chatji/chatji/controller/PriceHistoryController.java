package com.chatji.chatji.controller;

import com.chatji.chatji.domain.price.PriceHistory;
import com.chatji.chatji.domain.price.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/price-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PriceHistoryController {

    private final PriceHistoryRepository priceHistoryRepository;

    /**
     * v27: 특정 상품의 시세 히스토리 조회
     * 프론트엔드 그래프 컴포넌트에서 호출하여 시각화함
     */
    @GetMapping("/{productId}")
    public List<PriceHistory> getHistory(@PathVariable String productId) {
        return priceHistoryRepository.findByProductIdOrderByTimestampAsc(productId);
    }

    /**
     * 특정 키워드에 대한 최근 시장 평균 시위 확인용
     */
    @GetMapping("/keyword")
    public List<PriceHistory> getKeywordTrends(@RequestParam String q) {
        return priceHistoryRepository.findTop20ByKeywordOrderByTimestampDesc(q);
    }
}

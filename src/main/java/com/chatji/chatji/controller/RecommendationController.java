package com.chatji.chatji.controller;

import com.chatji.chatji.domain.action.UserAction;
import com.chatji.chatji.domain.action.UserActionRepository;
import com.chatji.chatji.domain.hotdeal.HotDeal;
import com.chatji.chatji.domain.hotdeal.HotDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final UserActionRepository userActionRepository;
    private final HotDealRepository hotDealRepository;

    /**
     * v28: 행동 정보 기록 (클릭 시 호출)
     */
    @PostMapping("/actions")
    public void recordAction(@RequestBody ActionRequest request) {
        userActionRepository.save(UserAction.builder()
                .userId(request.userId())
                .category(request.category())
                .actionType("CLICK")
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * v28: 유저 맞춤형 추천 핫딜 조회
     */
    @GetMapping
    public List<HotDeal> getRecommendations(@RequestParam String userId) {
        // 1. 유저의 선호 카테고리 TOP 3 추출
        List<String> topCategories = userActionRepository.findTopCategoriesByUserId(userId);
        
        if (topCategories.isEmpty()) {
            // 정보가 없으면 전체에서 점수 높은 순으로 반환
            return hotDealRepository.findAll().stream()
                    .sorted((a, b) -> (b.getScore() != null ? b.getScore() : 0) - (a.getScore() != null ? a.getScore() : 0))
                    .limit(10)
                    .toList();
        }

        // 2. 해당 카테고리 중 점수가 높은 핫딜 추출
        return hotDealRepository.findAll().stream()
                .filter(deal -> deal.getCategoryLarge() != null && topCategories.contains(deal.getCategoryLarge()))
                .sorted((a, b) -> (b.getScore() != null ? b.getScore() : 0) - (a.getScore() != null ? a.getScore() : 0))
                .limit(10)
                .toList();
    }

    public record ActionRequest(String userId, String category) {}
}

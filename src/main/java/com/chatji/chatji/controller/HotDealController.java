package com.chatji.chatji.controller;

import com.chatji.chatji.domain.hotdeal.HotDeal;
import com.chatji.chatji.domain.hotdeal.HotDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hotdeals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 프론트엔드 통신 허용
public class HotDealController {

    private final HotDealRepository hotDealRepository;

    /**
     * 메인 페이지용 핫딜 리스트 조회
     * 최신순으로 상위 10개만 반환
     */
    @GetMapping
    public List<HotDeal> getTopHotDeals() {
        return hotDealRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .limit(10)
                .toList();
    }
}

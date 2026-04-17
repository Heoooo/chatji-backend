package com.chatji.chatji.service;

import com.chatji.chatji.domain.hotdeal.HotDeal;
import com.chatji.chatji.domain.hotdeal.HotDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotDealCrawlerService {

    private final HotDealRepository hotDealRepository;
    private final ProductService productService; // 기존 최저가 검색 엔진 활용
    private final AlarmService alarmService;     // 알림 엔진

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void crawlPomppuHotDeals() {
        log.info("[v23-CRAWLER] Smart HotDeal Monitoring Started...");
        String url = "https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu";

        try {
            Document doc = Jsoup.connect(url).get();
            Elements rows = doc.select("tr.list0, tr.list1");

            for (Element row : rows) {
                Element titleElement = row.select("font.list_title").first();
                if (titleElement == null) continue;

                String title = titleElement.text();
                String link = "https://www.ppomppu.co.kr/zboard/" + row.select("a").first().attr("href");
                String originId = link.split("no=")[1].split("&")[0];

                if (hotDealRepository.findByOriginId(originId).isPresent()) continue;

                // 1. 가격 추출 알고리즘 (정규식 활용)
                Integer dealPrice = extractPrice(title);
                if (dealPrice == null || dealPrice < 1000) continue; // 가격 정보 없으면 스킵

                // 2. 스마트 감지 알고리즘: 네이버 최저가와 비교
                String keyword = cleanKeyword(title);
                List<ProductService.ProductResponse> naverResults = productService.searchProducts(keyword, "sim", 1, null, null);
                
                if (!naverResults.isEmpty()) {
                    int naverLowestPrice = naverResults.get(0).lprice();
                    int discountRate = (int) (((double)(naverLowestPrice - dealPrice) / naverLowestPrice) * 100);

                    // 3. 10% 이상 저렴한 '진짜 핫딜'만 수집 및 알림
                    if (discountRate >= 10) {
                        HotDeal newDeal = HotDeal.builder()
                                .originId(originId)
                                .title(title)
                                .url(link)
                                .currentPrice(dealPrice)
                                .source("뽐뿌")
                                .build();

                        hotDealRepository.save(newDeal);
                        alarmService.sendHotDealAlarm(newDeal, discountRate);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[v23-ERROR] Crawler: {}", e.getMessage());
        }
    }

    private Integer extractPrice(String title) {
        // 숫자와 '원'이 결합된 형태 추출 (예: 15,000원 -> 15000)
        Pattern pattern = Pattern.compile("([\\d,]+)원");
        Matcher matcher = pattern.matcher(title.replace(" ", ""));
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1).replace(",", ""));
        }
        return null;
    }

    private String cleanKeyword(String title) {
        // 제목에서 광고성 문구 제거하여 검색어 정제
        return title.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").trim();
    }
}

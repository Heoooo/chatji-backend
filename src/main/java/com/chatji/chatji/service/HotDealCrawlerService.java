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
    private final ProductService productService;
    private final AlarmService alarmService;

    /**
     * 멀티 타겟 크롤링 엔진 (v24)
     * - 뽐뿌 & 루리웹 동시 감시
     * - 검증 임계치 완화 (10% -> 1%)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 1000)
    @Transactional
    public void crawlAllHotDeals() {
        log.info("[v24-CRAWLER] Multi-target Hunting Started (Pomppu & Ruliweb)");
        
        // 1. 뽐뿌 사냥
        crawlPomppu();
        
        // 2. 루리웹 사냥
        crawlRuliweb();
    }

    private void crawlPomppu() {
        String url = "https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu";
        try {
            Document doc = Jsoup.connect(url).get();
            Elements rows = doc.select("tr.list0, tr.list1");
            processRows(rows, "뽐ppu", "no=");
        } catch (Exception e) {
            log.error("[v24] Pomppu Error: {}", e.getMessage());
        }
    }

    private void crawlRuliweb() {
        String url = "https://bbs.ruliweb.com/market/board/1020";
        try {
            Document doc = Jsoup.connect(url).get();
            Elements rows = doc.select("tr.table_body");
            for (Element row : rows) {
                Element titleElement = row.select("a.deco").first();
                if (titleElement == null) continue;

                String title = titleElement.text();
                String link = titleElement.attr("href");
                String originId = link.substring(link.lastIndexOf("/") + 1);

                processSingleDeal(title, link, originId, "루리웹");
            }
        } catch (Exception e) {
            log.error("[v24] Ruliweb Error: {}", e.getMessage());
        }
    }

    private void processRows(Elements rows, String source, String idParam) {
        for (Element row : rows) {
            Element titleElement = row.select("font.list_title").first();
            if (titleElement == null) continue;

            String title = titleElement.text();
            Element linkElement = row.select("a").first();
            if (linkElement == null) continue;
            
            String link = "https://www.ppomppu.co.kr/zboard/" + linkElement.attr("href");
            String originId = link.contains(idParam) ? link.split(idParam)[1].split("&")[0] : link;

            processSingleDeal(title, link, originId, source);
        }
    }

    private void processSingleDeal(String title, String link, String originId, String source) {
        if (hotDealRepository.findByOriginId(originId).isPresent()) return;

        Integer dealPrice = extractPrice(title);
        // 테스트를 위해 가격 정보가 없어도 일단 수집 (임계치 완화 스토리)
        if (dealPrice == null) dealPrice = 0; 

        String keyword = cleanKeyword(title);
        List<ProductService.ProductResponse> naverResults = productService.searchProducts(keyword, "sim", 1, null, null);
        
        int discountRate = 0;
        if (!naverResults.isEmpty()) {
            int naverLowestPrice = naverResults.get(0).lprice();
            if (dealPrice > 0) {
                discountRate = (int) (((double)(naverLowestPrice - dealPrice) / naverLowestPrice) * 100);
            }
        }

        // 임계치를 1%로 대폭 완화하여 수율 확보
        if (discountRate >= 1 || dealPrice == 0) {
            HotDeal newDeal = HotDeal.builder()
                    .originId(originId)
                    .title(title)
                    .url(link)
                    .currentPrice(dealPrice)
                    .source(source)
                    .build();

            hotDealRepository.save(newDeal);
            alarmService.sendHotDealAlarm(newDeal, discountRate);
        }
    }

    private Integer extractPrice(String title) {
        Pattern pattern = Pattern.compile("([\\d,]+)원");
        Matcher matcher = pattern.matcher(title.replace(" ", "").replace(",", ""));
        if (matcher.find()) {
            try { return Integer.parseInt(matcher.group(1)); } catch (Exception e) { return null; }
        }
        return null;
    }

    private String cleanKeyword(String title) {
        return title.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").trim();
    }
}

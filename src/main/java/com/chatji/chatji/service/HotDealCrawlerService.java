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

    @Scheduled(fixedDelay = 300000, initialDelay = 1000)
    @Transactional
    public void crawlAllHotDeals() {
        log.info("[v26-CRAWLER] Multi-target Hunting Started (Pomppu & Ruliweb)");
        crawlPomppu();
        crawlRuliweb();
    }

    private void crawlPomppu() {
        String url = "https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu";
        try {
            Document doc = Jsoup.connect(url).get();
            Elements rows = doc.select("tr.list0, tr.list1");
            processRows(rows, "뽐뿌", "no=");
        } catch (Exception e) {
            log.error("[v26] Pomppu Error: {}", e.getMessage());
        }
    }

    private void crawlRuliweb() {
        String url = "https://bbs.ruliweb.com/market/board/1020";
        try {
            Document doc = Jsoup.connect(url).get();
            Elements rows = doc.select("tr.table_body");
            for (Element row : rows) {
                try {
                    Element titleElement = row.select("a.deco").first();
                    if (titleElement == null) continue;

                    String title = titleElement.text();
                    String link = titleElement.attr("href");
                    String originId = link.contains("read/") ? link.split("read/")[1].split("\\?")[0] : link;

                    processSingleDeal(title, link, originId, "루리웹");
                    Thread.sleep(300); // 429 에러 방지용 (안전히 0.3초)
                } catch (Exception e) { continue; }
            }
        } catch (Exception e) {
            log.error("[v26] Ruliweb Error: {}", e.getMessage());
        }
    }

    private void processRows(Elements rows, String source, String idParam) {
        for (Element row : rows) {
            try {
                Element titleElement = row.select("font.list_title").first();
                if (titleElement == null) continue;

                String title = titleElement.text();
                Element linkElement = row.select("a").first();
                if (linkElement == null) continue;
                
                String link = "https://www.ppomppu.co.kr/zboard/" + linkElement.attr("href");
                String originId = link.contains(idParam) ? link.split(idParam)[1].split("&")[0] : link;

                processSingleDeal(title, link, originId, source);
                Thread.sleep(300); 
            } catch (Exception e) { continue; }
        }
    }

    private void processSingleDeal(String title, String link, String originId, String source) {
        if (hotDealRepository.findByOriginId(originId).isPresent()) return;

        Integer dealPrice = extractPrice(title);
        if (dealPrice == null || dealPrice == 0) return; // 0원 딜은 수집하지 않음 (정확도 우선)

        String keyword = cleanKeyword(title);
        List<ProductService.ProductResponse> naverResults = productService.searchProducts(keyword, "sim", 1, null, null);
        
        int discountRate = 0;
        if (!naverResults.isEmpty()) {
            int naverLowestPrice = naverResults.get(0).lprice();
            discountRate = (int) (((double)(naverLowestPrice - dealPrice) / naverLowestPrice) * 100);
        }

        // 진짜 핫딜(10% 이상 저렴)로 판명될 때만 저장
        if (discountRate >= 10) { 
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
        // 모든 공백 제거 후 가격 패턴 분석
        String cleanTitle = title.replace(" ", "").replace(",", "");
        
        // 1. 숫자 + 원 (가장 일반적)
        Pattern p1 = Pattern.compile("(\\d{3,10})원");
        Matcher m1 = p1.matcher(cleanTitle);
        if (m1.find()) return Integer.parseInt(m1.group(1));

        // 2. 괄호 안의 숫자 (루리웹 다수)
        Pattern p2 = Pattern.compile("\\((\\d{4,10})");
        Matcher m2 = p2.matcher(cleanTitle);
        if (m2.find()) return Integer.parseInt(m2.group(1));

        // 3. 상품명 끝의 가격 (뽐뿌 다수)
        Pattern p3 = Pattern.compile("/(\\d{4,10})");
        Matcher m3 = p3.matcher(cleanTitle);
        if (m3.find()) return Integer.parseInt(m3.group(1));

        return null;
    }

    private String cleanKeyword(String title) {
        return title.replaceAll("\\[.*?\\]", "")
                    .replaceAll("\\(.*?\\)", "")
                    .replaceAll("[\\d,]+원?", "")
                    .replaceAll("/[\\d,]+", "")
                    .trim();
    }
}

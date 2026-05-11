package com.chatji.chatji;

import com.chatji.chatji.domain.alert.Alert;
import com.chatji.chatji.domain.alert.AlertRepository;
import com.chatji.chatji.domain.hotdeal.HotDeal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
public class BenchmarkTest {

    @Autowired
    private AlertRepository alertRepository;

    @Test
    public void runAlertMatchingBenchmark() {
        int testCount = 10000;
        System.out.println("Starting Benchmark with " + testCount + " alerts...");

        // 1. 데이터 준비
        alertRepository.deleteAll();
        List<Alert> dummyAlerts = new ArrayList<>();
        for (int i = 0; i < testCount; i++) {
            dummyAlerts.add(Alert.builder()
                    .userId("user_" + i)
                    .type(i % 2 == 0 ? Alert.AlertType.KEYWORD : Alert.AlertType.CATEGORY)
                    .keyword(i % 2 == 0 ? "맥북" + (i % 100) : null) // 100종류의 키워드 반복
                    .categoryLarge(i % 2 != 0 ? "가전" : null)
                    .categorySmall(i % 2 != 0 ? "노트북" : null)
                    .active(true)
                    .build());
        }
        alertRepository.saveAll(dummyAlerts);

        HotDeal testDeal = HotDeal.builder()
                .title("애플 2024 맥북 에어 M3 실버 13인치 특가")
                .categoryLarge("가전")
                .categorySmall("노트북")
                .build();

        // 2. 기존 방식 (Memory Loop)
        long startOld = System.nanoTime();
        List<Alert> all = alertRepository.findAllByActiveTrue();
        int matchCountOld = 0;
        for (Alert a : all) {
            if (a.getType() == Alert.AlertType.KEYWORD && a.getKeyword() != null) {
                if (testDeal.getTitle().contains(a.getKeyword())) matchCountOld++;
            } else if (a.getType() == Alert.AlertType.CATEGORY) {
                if (testDeal.getCategorySmall().equals(a.getCategorySmall())) matchCountOld++;
            }
        }
        long endOld = System.nanoTime();
        double timeOld = (endOld - startOld) / 1_000_000.0;

        // 3. 최적화 방식 (DB Query)
        long startNew = System.nanoTime();
        List<Alert> matchedNew = alertRepository.findMatchingAlerts(
                testDeal.getTitle(), testDeal.getCategoryLarge(), testDeal.getCategorySmall()
        );
        long endNew = System.nanoTime();
        double timeNew = (endNew - startNew) / 1_000_000.0;

        double improvement = ((timeOld - timeNew) / timeOld) * 100;

        System.out.println("\n" + "=".repeat(40));
        System.out.println("   BENCHMARK RESULT (10,000 Alerts)");
        System.out.println("=".repeat(40));
        System.out.printf("Old Method (Loop):   %.2f ms\n", timeOld);
        System.out.printf("New Method (Query):  %.2f ms\n", timeNew);
        System.out.printf("Improvement:         %.2f%%\n", improvement);
        System.out.printf("Matched Users:       %d\n", matchedNew.size());
        System.out.println("=".repeat(40));
    }
}

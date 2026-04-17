package com.chatji.chatji.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SynonymService {

    // 동의어 사전 (나중에 DB나 설정 파일로 분리 가능)
    private static final Map<String, List<String>> SYNONYM_MAP = new HashMap<>();

    static {
        SYNONYM_MAP.put("자켓", List.of("블루종", "점퍼", "jacket"));
        SYNONYM_MAP.put("노트북", List.of("laptop", "랩탑", "맥북"));
        SYNONYM_MAP.put("아이폰", List.of("iphone", "apple phone"));
        SYNONYM_MAP.put("청바지", List.of("데님", "jeans", "팬츠"));
        SYNONYM_MAP.put("셔츠", List.of("남방", "shirt", "블라우스"));
    }

    /**
     * 입력된 키워드에 대한 동의어 목록을 반환합니다. (자기 자신 포함)
     */
    public List<String> expandKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase().trim();
        List<String> synonyms = new ArrayList<>();
        synonyms.add(keyword); // 원본 키워드 포함

        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            if (entry.getKey().equals(lowerKeyword) || entry.getValue().contains(lowerKeyword)) {
                synonyms.addAll(entry.getValue());
                if (!entry.getKey().equals(lowerKeyword)) {
                    synonyms.add(entry.getKey());
                }
                break;
            }
        }

        // 중복 제거 및 원본 제외 최대 3개까지만 확장 (성능 고려)
        return synonyms.stream()
                .distinct()
                .limit(3)
                .toList();
    }
}

package com.chatji.chatji.controller;

import com.chatji.chatji.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin("*") // 간단한 연동을 위한 CORS 허용
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductService.ProductResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "sim") String sort,
            @RequestParam(defaultValue = "1") int start,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice) {

        long startTime = System.currentTimeMillis();
        List<ProductService.ProductResponse> results = productService.searchProducts(keyword, sort, start, minPrice,
                maxPrice);
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("[PROVE-CACHE] Total Response Time: {}ms", duration);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<String> getSuggestions(@RequestParam String q) {
        try {
            RestClient restClient = RestClient.create();
            String response = restClient.get()
                    .uri("https://ac.shopping.naver.com/ac?q=" + q
                            + "&st=111&r_lt=111&r_format=json&r_enc=UTF-8&frm=shopping")
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Referer", "https://shopping.naver.com/")
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);

        } catch (Exception e) {
            try {
                String fallback = RestClient.create().get()
                        .uri("https://suggestqueries.google.com/complete/search?ds=sh&client=firefox&q=" + q)
                        .retrieve()
                        .body(String.class);
                return ResponseEntity.ok()
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .body(fallback);
            } catch (Exception ex) {
                return ResponseEntity.ok("{\"items\":[[]]}");
            }
        }
    }

}

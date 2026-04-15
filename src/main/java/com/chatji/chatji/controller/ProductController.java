package com.chatji.chatji.controller;

import com.chatji.chatji.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestParam(defaultValue = "1") int start) {

        List<ProductService.ProductResponse> results = productService.searchProducts(keyword, sort, start);
        return ResponseEntity.ok(results);
    }
}

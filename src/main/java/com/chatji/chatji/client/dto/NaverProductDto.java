package com.chatji.chatji.client.dto;

import java.util.List;

public record NaverProductDto(
        int total,
        int start,
        int display,
        List<Item> items) {
    public record Item(
            String title,
            String link,
            String image,
            int lprice,
            String mallName,
            String productId) {
    }
}

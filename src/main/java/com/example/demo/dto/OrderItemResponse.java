package com.example.demo.dto;

import java.math.BigDecimal;

/** Una línea del pedido. {@code lineTotal} se calcula al mapear, no se guarda. */
public record OrderItemResponse(Long productId, String title, BigDecimal unitPrice, int quantity,
        String imageUrl, BigDecimal lineTotal) {
}

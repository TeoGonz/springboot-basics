package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.example.demo.entity.OrderStatus;

/**
 * Pedido completo. No lleva id de usuario: describe al que pregunta, que ya sabe
 * quién es.
 */
public record OrderDetailResponse(Long id, OrderStatus status, BigDecimal total,
        String recipientName, String address, String phone, Instant createdAt, Instant updatedAt,
        List<OrderItemResponse> items) {
}

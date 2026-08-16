package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.demo.entity.OrderStatus;

/**
 * Pedido en la lista del cliente, sin las líneas.
 *
 * <p>{@code itemCount} es el número de líneas, no la suma de cantidades: etiqueta
 * una lista («3 productos»), y las cantidades se ven al abrir el detalle.
 */
public record OrderSummaryResponse(Long id, OrderStatus status, BigDecimal total, int itemCount,
        Instant createdAt, Instant updatedAt) {
}

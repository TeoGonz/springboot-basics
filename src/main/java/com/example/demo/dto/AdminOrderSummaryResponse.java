package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.demo.entity.OrderStatus;

/**
 * Pedido en la lista de administración.
 *
 * <p>Lleva {@code username} y <b>nunca el correo</b>: esta es una pantalla de
 * operación, no un listado de clientes. La dirección sirve para enviar, no para
 * mostrarse.
 */
public record AdminOrderSummaryResponse(Long id, String username, OrderStatus status,
        BigDecimal total, int itemCount, Instant createdAt, Instant updatedAt) {
}

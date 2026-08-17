package com.example.demo.dto;

import java.time.Instant;

import com.example.demo.entity.OrderStatus;

/**
 * Lo que viaja por el stream de seguimiento: qué pedido cambió, a qué estado y
 * cuándo.
 *
 * <p>Tres campos a propósito. El evento dice <b>qué cambió</b>, no cómo es el
 * pedido: ni total, ni dirección, ni líneas. Un canal que puede quedarse abierto
 * un cuarto de hora debe llevar lo mínimo, y la página ya tiene el resto.
 */
public record OrderStatusEvent(Long orderId, OrderStatus status, Instant at) {

    /** Primer evento de cada conexión: el estado actual, tal y como se leyó. */
    public static OrderStatusEvent of(OrderDetailResponse order) {
        return new OrderStatusEvent(order.id(), order.status(), order.updatedAt());
    }
}

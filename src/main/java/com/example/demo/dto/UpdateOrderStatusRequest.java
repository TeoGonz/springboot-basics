package com.example.demo.dto;

import com.example.demo.entity.OrderStatus;

import jakarta.validation.constraints.NotNull;

/** Cambio de estado de un pedido. Solo avanza; el servicio rechaza lo demás. */
public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}

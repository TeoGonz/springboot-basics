package com.example.demo.entity;

/**
 * Estados por los que pasa un pedido, en orden.
 *
 * <p>El avance es <b>solo hacia adelante</b>: se puede saltar un paso
 * ({@code PREPARING → DELIVERED}), pero nunca retroceder ni repetir el estado
 * actual. Repetirlo se rechaza a propósito — un doble clic en el formulario del
 * administrador enviaría, si no, un segundo correo al cliente diciendo lo mismo.
 *
 * <p>No hay {@code CANCELLED}: el requisito nombra tres estados, y añadir uno
 * cuarto obliga a decidir qué significa cancelar algo ya entregado.
 */
public enum OrderStatus {

    PREPARING,
    SHIPPED,
    DELIVERED;

    /** Solo es legal avanzar en el orden de declaración. */
    public boolean canMoveTo(OrderStatus next) {
        return next != null && next.ordinal() > this.ordinal();
    }
}

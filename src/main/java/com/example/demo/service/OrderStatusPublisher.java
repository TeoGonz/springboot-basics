package com.example.demo.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.dto.OrderStatusEvent;
import com.example.demo.entity.OrderStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Reparte los cambios de estado a las conexiones abiertas.
 *
 * <p>Se publica <b>desde el controlador, después de que la transacción confirme</b>
 * — nunca dentro de {@code OrderService}. Es la misma regla que ya sigue el
 * correo y por el mismo motivo: un evento emitido dentro de la transacción puede
 * anunciar un estado que luego se deshace, y de una pantalla que ya se movió no
 * se retira.
 *
 * <p>Un solo sink para toda la aplicación, no uno por pedido: un mapa de sinks
 * necesitaría desalojo (los de pedidos que nadie mira se acumularían), y la
 * alternativa cuesta un {@code filter} por suscriptor sobre un flujo que emite
 * un puñado de eventos al día.
 */
@Service
public class OrderStatusPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusPublisher.class);

    /**
     * {@code multicast} porque varias pestañas pueden mirar el mismo pedido.
     *
     * <p>{@code directBestEffort} y no {@code onBackpressureBuffer}: el que
     * almacena se «calienta» — sin suscriptores encola y se lo reproduce al
     * primero que llegue, lo que en un estado que solo avanza repintaría uno
     * <i>anterior</i> en una conexión recién abierta. El directo no guarda nada,
     * así que tampoco puede crecer en memoria. Un suscriptor demasiado lento
     * pierde ese evento, no la conexión: el estado real se reenvía en cuanto
     * reconecte.
     */
    private final Sinks.Many<OrderStatusEvent> sink = Sinks.many().multicast().directBestEffort();

    /**
     * {@code synchronized} porque {@code tryEmitNext} falla con
     * {@code FAIL_NON_SERIALIZED} si dos hilos emiten a la vez, que es lo que
     * provocarían dos administradores actuando al mismo tiempo. Ya serializado,
     * el único fallo alcanzable es {@code FAIL_ZERO_SUBSCRIBER} — nadie mirando,
     * el caso normal.
     */
    public synchronized void publish(Long orderId, OrderStatus status, Instant at) {
        Sinks.EmitResult result = sink.tryEmitNext(new OrderStatusEvent(orderId, status, at));
        if (result.isFailure()) {
            log.debug("pedido {} -> {}: evento no emitido ({})", orderId, status, result);
        }
    }

    /** Flujo compartido. Cada suscriptor filtra por el pedido que mira. */
    public Flux<OrderStatusEvent> stream() {
        return sink.asFlux();
    }
}

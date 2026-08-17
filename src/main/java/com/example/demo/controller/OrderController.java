package com.example.demo.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDetailResponse;
import com.example.demo.dto.OrderStatusEvent;
import com.example.demo.dto.OrderSummaryResponse;
import com.example.demo.entity.OrderStatus;
import com.example.demo.service.MailService;
import com.example.demo.service.OrderService;
import com.example.demo.service.OrderService.OrderResult;
import com.example.demo.service.OrderStatusPublisher;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Pedidos del cliente: registrarlos, consultarlos y seguirlos en vivo.
 *
 * <p>Quién compra sale siempre del {@link Authentication}, nunca del cuerpo. Cae
 * bajo {@code anyRequest().authenticated()}, así que la cadena de seguridad no
 * necesita ninguna regla nueva — tampoco para el stream: lo raro ahí es la
 * respuesta, no la petición.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final MailService mailService;
    private final OrderStatusPublisher statusPublisher;
    private final Duration heartbeatInterval;
    private final Duration maxStreamDuration;

    public OrderController(OrderService orderService, MailService mailService,
            OrderStatusPublisher statusPublisher,
            @Value("${app.orders.stream.heartbeat-ms}") long heartbeatMs,
            @Value("${app.orders.stream.max-duration-ms}") long maxDurationMs) {
        this.orderService = orderService;
        this.mailService = mailService;
        this.statusPublisher = statusPublisher;
        this.heartbeatInterval = Duration.ofMillis(heartbeatMs);
        this.maxStreamDuration = Duration.ofMillis(maxDurationMs);
    }

    @PostMapping
    public ResponseEntity<OrderDetailResponse> create(Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResult result = orderService.create(authentication.getName(), request);

        // Fuera del @Transactional a propósito: el pedido ya está confirmado, así
        // que ningún correo puede describir algo que después se deshace. El envío
        // es @Async y su fallo no toca esta respuesta.
        mailService.sendOrderConfirmation(result.email(), result.username(), result.order().id(),
                result.order().total(), result.lines(), result.locale());

        return ResponseEntity.status(HttpStatus.CREATED).body(result.order());
    }

    @GetMapping
    public List<OrderSummaryResponse> mine(Authentication authentication) {
        return orderService.listMine(authentication.getName());
    }

    /** El pedido de otro contesta 404, igual que uno inexistente. */
    @GetMapping("/{id}")
    public OrderDetailResponse one(Authentication authentication, @PathVariable Long id) {
        return orderService.findMine(authentication.getName(), id);
    }

    /**
     * Seguimiento en vivo: la conexión queda abierta y recibe cada cambio de
     * estado en cuanto el administrador lo hace.
     *
     * <p>Esto <b>no es WebFlux</b>. El stack sigue siendo Servlet; Spring MVC
     * sabe adaptar un {@link Flux} a una respuesta asíncrona, y el hilo de
     * petición se libera en cuanto la petición pasa a async — un stream abierto
     * no ocupa ninguno.
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<OrderStatusEvent>> stream(Authentication authentication,
            @PathVariable Long id) {

        // Antes de construir ningún Flux: esto puede lanzar, y una ApiException
        // solo puede convertirse en el JSON de error de siempre mientras no se
        // haya escrito la cabecera de la respuesta. Comprobando aquí, un id
        // ajeno o inexistente da el mismo 404 que GET /api/orders/{id}. De paso
        // devuelve el estado actual, que es justo el primer evento.
        OrderDetailResponse current = orderService.findMine(authentication.getName(), id);

        Flux<ServerSentEvent<OrderStatusEvent>> data = Flux
                .concat(Mono.just(OrderStatusEvent.of(current)),
                        statusPublisher.stream().filter(event -> id.equals(event.orderId())))
                .map(event -> ServerSentEvent.builder(event).event("status").build());

        // Un frame de solo comentario es el latido estándar. Sin él, un proxy
        // corta la conexión inactiva y un cliente que desapareció sin cerrar no
        // se detecta hasta la siguiente escritura.
        Flux<ServerSentEvent<OrderStatusEvent>> heartbeat = Flux.interval(heartbeatInterval)
                .map(tick -> ServerSentEvent.<OrderStatusEvent>builder().comment("keep-alive").build());

        // El corte se decide mirando el evento ya emitido, no suscribiéndose una
        // segunda vez al sink: dos suscripciones compiten, y la que solo vigila
        // podría cancelar el stream antes de que la otra llegue a escribir el
        // DELIVERED. Al completar aquí, el latido se cancela con los datos.
        return Flux.merge(data, heartbeat)
                .takeUntil(sse -> sse.data() != null && sse.data().status() == OrderStatus.DELIVERED)
                // Cortar a propósito, en vez de dejar que el contenedor aborte
                // por timeout y registre una excepción por algo normal.
                .take(maxStreamDuration);
    }
}

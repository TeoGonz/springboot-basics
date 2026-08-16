package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDetailResponse;
import com.example.demo.dto.OrderSummaryResponse;
import com.example.demo.service.MailService;
import com.example.demo.service.OrderService;
import com.example.demo.service.OrderService.OrderResult;

import jakarta.validation.Valid;

/**
 * Pedidos del cliente: registrarlos y consultarlos.
 *
 * <p>Quién compra sale siempre del {@link Authentication}, nunca del cuerpo. Cae
 * bajo {@code anyRequest().authenticated()}, así que la cadena de seguridad no
 * necesita ninguna regla nueva.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final MailService mailService;

    public OrderController(OrderService orderService, MailService mailService) {
        this.orderService = orderService;
        this.mailService = mailService;
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
}

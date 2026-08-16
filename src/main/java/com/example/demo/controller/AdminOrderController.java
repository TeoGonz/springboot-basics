package com.example.demo.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AdminOrderSummaryResponse;
import com.example.demo.dto.OrderDetailResponse;
import com.example.demo.dto.UpdateOrderStatusRequest;
import com.example.demo.entity.OrderStatus;
import com.example.demo.service.MailService;
import com.example.demo.service.OrderService;
import com.example.demo.service.OrderService.OrderResult;
import com.example.demo.web.ApiException;

import jakarta.validation.Valid;

/**
 * Gestión de pedidos por parte del administrador: verlos todos y moverlos de
 * estado.
 *
 * <p>Cuelga de {@code /api/admin/**}, que la cadena de seguridad ya restringe a
 * {@code ROLE_ADMIN} — es el primer manejador que ese prefijo tiene.
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final MailService mailService;

    public AdminOrderController(OrderService orderService, MailService mailService) {
        this.orderService = orderService;
        this.mailService = mailService;
    }

    @GetMapping
    public List<AdminOrderSummaryResponse> list(@RequestParam(required = false) String status) {
        return orderService.listAll(parseStatus(status));
    }

    @PatchMapping("/{id}/status")
    public OrderDetailResponse changeStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResult result = orderService.changeStatus(id, request.status());

        // Igual que en el alta: la transacción ya confirmó cuando se avisa.
        mailService.sendOrderStatusChanged(result.email(), result.username(), result.order().id(),
                result.order().status(), result.locale());

        return result.order();
    }

    /**
     * Se recibe como texto y se convierte a mano para que un valor desconocido
     * salga con la forma de error de siempre, y no con la que Spring da a un
     * fallo de conversión.
     */
    private static OrderStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "unknown status: " + value);
        }
    }
}

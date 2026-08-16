package com.example.demo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AdminOrderSummaryResponse;
import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDetailResponse;
import com.example.demo.dto.OrderItemResponse;
import com.example.demo.dto.OrderSummaryResponse;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.web.ApiException;

/**
 * Ciclo de vida del pedido: registrarlo, consultarlo y moverlo de estado.
 *
 * <p>Esta clase <b>no envía correo</b>. Lo hace el controlador cuando el método
 * ya ha devuelto y la transacción ha confirmado: un correo mandado dentro de la
 * transacción puede acabar describiendo un pedido que después se deshace, y no
 * hay forma de retirarlo del buzón del cliente. Por eso {@link OrderResult}
 * devuelve, junto a la respuesta HTTP, lo que el aviso necesita.
 */
@Service
public class OrderService {

    private static final String DEFAULT_LOCALE = "es";

    private final OrderRepository orders;
    private final UserRepository users;

    public OrderService(OrderRepository orders, UserRepository users) {
        this.orders = orders;
        this.users = users;
    }

    /**
     * Registra un pedido a nombre del usuario autenticado. El total se calcula
     * <b>aquí</b>, desde las líneas; el cliente nunca lo propone.
     */
    @Transactional
    public OrderResult create(String username, CreateOrderRequest request) {
        User user = requireUser(username);

        // Dos líneas del mismo producto son un fallo del carrito, no motivo para
        // rechazar el pedido ni para imprimir un recibo con la entrada repetida.
        // Gana la primera aparición: título, precio e imagen son los que se vieron.
        Map<Long, CreateOrderRequest.Item> collapsed = new LinkedHashMap<>();
        for (CreateOrderRequest.Item item : request.items()) {
            collapsed.merge(item.productId(), item, OrderService::sumQuantities);
        }
        if (collapsed.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_CART", "order has no items");
        }

        Order order = new Order(user, normalizeLocale(request.locale()), request.recipientName(),
                request.address(), request.phone());
        for (CreateOrderRequest.Item item : collapsed.values()) {
            order.addItem(new OrderItem(item.productId(), item.title(), item.unitPrice(),
                    item.quantity(), item.imageUrl()));
        }
        order.recalculateTotal();

        return result(orders.save(order));
    }

    /** Los pedidos del propio cliente, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listMine(String username) {
        return orders.findByUserIdOrderByCreatedAtDesc(requireUser(username).getId()).stream()
                .map(OrderService::toSummary)
                .toList();
    }

    /**
     * Un pedido del propio cliente. El de otro contesta lo mismo que uno que no
     * existe: si el ajeno diera 403 y el inexistente 404, las dos respuestas
     * juntas confirmarían qué identificadores son reales.
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse findMine(String username, Long id) {
        return orders.findByIdAndUserId(id, requireUser(username).getId())
                .map(OrderService::toDetail)
                .orElseThrow(OrderService::orderNotFound);
    }

    /** Todos los pedidos, opcionalmente filtrados por estado. Solo administración. */
    @Transactional(readOnly = true)
    public List<AdminOrderSummaryResponse> listAll(OrderStatus status) {
        List<Order> found = (status == null)
                ? orders.findAllByOrderByCreatedAtDesc()
                : orders.findByStatusOrderByCreatedAtDesc(status);
        return found.stream().map(OrderService::toAdminSummary).toList();
    }

    /** Avanza el estado. Retroceder o repetir el actual es 409. */
    @Transactional
    public OrderResult changeStatus(Long id, OrderStatus next) {
        Order order = orders.findById(id).orElseThrow(OrderService::orderNotFound);

        if (!order.getStatus().canMoveTo(next)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "cannot move from %s to %s".formatted(order.getStatus(), next));
        }
        order.setStatus(next);

        // Se fuerza el flush para que @UpdateTimestamp esté escrito antes de mapear:
        // si no, la respuesta llevaría la fecha de la modificación anterior.
        return result(orders.saveAndFlush(order));
    }

    private User requireUser(String username) {
        return users.findByUsername(username).orElseThrow(() -> new IllegalStateException(
                "El token nombra a un usuario que ya no existe: " + username));
    }

    private static CreateOrderRequest.Item sumQuantities(CreateOrderRequest.Item first,
            CreateOrderRequest.Item repeated) {
        return new CreateOrderRequest.Item(first.productId(), first.title(), first.unitPrice(),
                first.quantity() + repeated.quantity(), first.imageUrl());
    }

    private static String normalizeLocale(String locale) {
        return (locale == null || locale.isBlank()) ? DEFAULT_LOCALE : locale;
    }

    private static ApiException orderNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "unknown order");
    }

    private static OrderResult result(Order order) {
        return new OrderResult(toDetail(order), order.getUser().getEmail(),
                order.getUser().getUsername(), Locale.forLanguageTag(order.getLocale()),
                order.getItems().stream()
                        .map(item -> "%d × %s".formatted(item.getQuantity(), item.getTitle()))
                        .toList());
    }

    private static OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getStatus(), order.getTotal(),
                order.getItems().size(), order.getCreatedAt(), order.getUpdatedAt());
    }

    private static AdminOrderSummaryResponse toAdminSummary(Order order) {
        return new AdminOrderSummaryResponse(order.getId(), order.getUser().getUsername(),
                order.getStatus(), order.getTotal(), order.getItems().size(), order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private static OrderDetailResponse toDetail(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getProductId(), item.getTitle(),
                        item.getUnitPrice(), item.getQuantity(), item.getImageUrl(),
                        item.lineTotal()))
                .toList();
        return new OrderDetailResponse(order.getId(), order.getStatus(), order.getTotal(),
                order.getRecipientName(), order.getAddress(), order.getPhone(),
                order.getCreatedAt(), order.getUpdatedAt(), items);
    }

    /**
     * El pedido tal como sale por HTTP más lo que hace falta para avisar al
     * cliente. Solo se serializa {@code order()}; el resto muere en el controlador.
     */
    public record OrderResult(OrderDetailResponse order, String email, String username,
            Locale locale, List<String> lines) {
    }
}

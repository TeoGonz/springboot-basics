package com.example.demo.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Pedido cerrado. El carrito no vive aquí: es un borrador del navegador y solo
 * llega a esta base cuando ya es un pedido.
 *
 * <p>La tabla se llama {@code orders} y no {@code order} porque {@code ORDER} es
 * palabra reservada en SQL: dejarlo al nombre por defecto obligaría a entrecomillar
 * la tabla en cada consulta escrita a mano.
 *
 * <p>{@code total} se <b>guarda</b>, no se recalcula al leer. Es lo que se le dijo
 * al cliente que iba a pagar; recalcularlo dejaría que un cambio futuro en la
 * fórmula reescribiera pedidos viejos, justo lo que evita congelar las líneas.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Perezoso: listar los pedidos de un cliente nunca necesita su fila de
     * usuario. La lista del administrador sí quiere el nombre, y lo pide con un
     * join explícito en el repositorio.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PREPARING;

    /**
     * Idioma en que se hizo el pedido: elige el de los correos, nada más. Es un
     * {@code varchar(2)} validado en el DTO y no un enum, para no atarse al
     * {@code PostLocale} de otro spec.
     */
    @Column(nullable = false, length = 2)
    private String locale;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 30)
    private String phone;

    /** {@code numeric(10,2)}: el dinero nunca pasa por {@code double}. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
        // requerido por JPA
    }

    public Order(User user, String locale, String recipientName, String address, String phone) {
        this.user = Objects.requireNonNull(user, "user");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName");
        this.address = Objects.requireNonNull(address, "address");
        this.phone = Objects.requireNonNull(phone, "phone");
    }

    /** Añade una línea manteniendo los dos lados de la relación en pie. */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Recalcula el total desde las líneas que este pedido tiene ahora mismo. Se
     * llama una vez, al crearlo: después el pedido ya no cambia de contenido.
     */
    public void recalculateTotal() {
        this.total = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getLocale() {
        return locale;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

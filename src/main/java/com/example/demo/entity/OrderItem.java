package com.example.demo.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Una línea del pedido, congelada en el momento de la compra.
 *
 * <p>Guarda una <b>copia</b> — título, precio, cantidad e imagen — y no solo el
 * {@code product_id}. El catálogo es un sandbox público que cualquiera reescribe:
 * los productos se borran, se renombran y cambian de precio. Un pedido es un
 * registro histórico, así que releer el catálogo al mostrarlo dejaría el recibo
 * ilegible o, peor, reescribiría lo que el cliente pagó. {@code product_id} queda
 * solo como enlace de vuelta.
 */
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    /** Puede faltar: hay productos del catálogo cuya imagen no es ni una URL. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    protected OrderItem() {
        // requerido por JPA
    }

    public OrderItem(Long productId, String title, BigDecimal unitPrice, int quantity,
            String imageUrl) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.title = Objects.requireNonNull(title, "title");
        // A dos decimales al entrar, no al guardar: si no, la respuesta del alta
        // devolvería "55" y la consulta posterior "55.00" para el mismo precio.
        // El DTO ya limita a dos decimales, así que aquí no se pierde nada.
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice")
                .setScale(2, RoundingMode.HALF_UP);
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    /**
     * No se guarda en columna: al contrario que {@code total}, es función pura de
     * dos campos de esta misma fila, así que no puede desincronizarse.
     */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public Long getProductId() {
        return productId;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}

package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Los pedidos del propio cliente. Trae las líneas: el resumen las cuenta. */
    @EntityGraph(attributePaths = "items")
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * La propiedad se comprueba <b>en la consulta</b>, no cargando la fila y
     * comparando después: así no hay camino de código que pueda olvidarse de
     * mirar de quién es el pedido.
     */
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * Lista de administración. Pide {@code user} e {@code items} en el mismo
     * grafo para que mostrar el nombre y contar las líneas no cueste dos
     * consultas por fila.
     */
    @EntityGraph(attributePaths = { "user", "items" })
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = { "user", "items" })
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}

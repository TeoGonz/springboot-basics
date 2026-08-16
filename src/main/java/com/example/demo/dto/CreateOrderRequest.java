package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un pedido. <b>No lleva id de usuario y no lo llevará</b>: quién compra
 * sale del token, nunca del cuerpo. Con un campo así, cualquiera con un token
 * válido podría pedir en nombre de otro.
 *
 * <p>Las líneas viajan con su precio porque esta API no habla con el catálogo
 * (ver la limitación documentada en el README). Los topes de tamaño no son
 * decorativos: sin ellos una sola petición escribe filas sin límite.
 *
 * <p>{@code locale} solo elige el idioma del correo; si no llega, se envía en
 * español.
 */
public record CreateOrderRequest(

        @NotEmpty(message = "el pedido no tiene productos")
        @Size(max = 50, message = "demasiados productos")
        List<@Valid Item> items,

        @NotBlank @Size(max = 120) String recipientName,

        @NotBlank @Size(max = 200) String address,

        @NotBlank @Size(max = 30) String phone,

        @Pattern(regexp = "es|en|pt", message = "idioma no soportado") String locale) {

    /** Copia del producto en el momento de comprarlo, no un puntero al catálogo. */
    public record Item(

            @NotNull Long productId,

            @NotBlank @Size(max = 200) String title,

            @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal unitPrice,

            @Min(1) @Max(99) int quantity,

            @Size(max = 500) String imageUrl) {
    }
}

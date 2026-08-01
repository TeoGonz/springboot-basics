package com.example.demo.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Forma única de toda respuesta de error de la API.
 *
 * <p>{@code error} es un código estable y legible por máquina
 * ({@code BAD_CREDENTIALS}, {@code EMAIL_TAKEN}, …): es lo que el front usa para
 * elegir el mensaje traducido. {@code message} es para quien depura, nunca para
 * enseñar al usuario. {@code fields} solo aparece en errores de validación.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String error, String message, Map<String, String> fields) {

    public ApiError(int status, String error, String message) {
        this(status, error, message, null);
    }
}

package com.example.demo.web;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que ya sabe con qué estado y con qué código debe salir.
 * Lo traduce a JSON {@link ApiExceptionHandler}, así que los controladores no
 * construyen respuestas de error a mano.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Código estable que el front mapea a su propio texto traducido. */
    public String getCode() {
        return code;
    }
}

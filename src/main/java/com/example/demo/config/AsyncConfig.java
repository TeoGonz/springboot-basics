package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita {@code @Async}. Lo usa el envío de correos: mandar el enlace de
 * recuperación no debe alargar la respuesta de la petición que lo pidió.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

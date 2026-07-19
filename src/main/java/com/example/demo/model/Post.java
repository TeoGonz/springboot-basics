package com.example.demo.model;

import java.time.LocalDate;

/**
 * Entrada de la bitácora (learning log). Estática por ahora; en una próxima
 * iteración estas entradas provendrán de una API.
 *
 * @param tituloKey  clave i18n del título (resuelta en la vista con #{...})
 * @param resumenKey clave i18n del resumen
 * @param banner     sufijo de clase CSS del banner (i18n | guards | next)
 * @param icon       clase de bootstrap-icons (p. ej. "bi-translate")
 * @param badge      clase de color del badge de semana (p. ej. "text-bg-primary")
 * @param fecha      fecha de publicación; null si aún no está publicada
 * @param publicado  false = "Próximamente", tarjeta en preparación
 */
public record Post(
        String slug,
        int semana,
        LocalDate fecha,
        String tituloKey,
        String resumenKey,
        String banner,
        String icon,
        String badge,
        boolean publicado) {
}

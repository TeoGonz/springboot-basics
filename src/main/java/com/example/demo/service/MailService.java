package com.example.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.entity.OrderStatus;

/**
 * Envío de correos transaccionales.
 *
 * <p>Es la única parte del backend con textos traducidos: el correo lo redacta y
 * lo manda esta aplicación, así que el idioma es cosa suya. Las respuestas de la
 * API siguen devolviendo códigos y no frases.
 *
 * <p>Texto plano a propósito: Thymeleaf se fue con las vistas y no vale la pena
 * traerlo de vuelta para maquetar un enlace.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final MessageSource messages;
    private final String from;
    private final String frontendBaseUrl;

    public MailService(JavaMailSender mailSender, MessageSource messages,
            @Value("${app.mail.from}") String from,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.messages = messages;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
    }

    /**
     * Se envía fuera del hilo de la petición: así {@code /forgot-password} tarda
     * lo mismo exista o no el correo, y nadie puede deducir quién está
     * registrado cronometrando la respuesta.
     */
    @Async
    public void sendPasswordResetLink(String to, String username, String link, int minutes,
            Locale locale) {
        String body = String.join("\n\n",
                messages.getMessage("reset.greeting", new Object[] { username }, locale),
                messages.getMessage("reset.body", new Object[] { minutes }, locale),
                link,
                messages.getMessage("reset.ignore", null, locale));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(messages.getMessage("reset.subject", null, locale));
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Correo de recuperación enviado a {}", to);
        } catch (MailException ex) {
            // El usuario ya recibió su 202: aquí solo queda dejar rastro.
            log.error("No se pudo enviar el correo de recuperación a {}", to, ex);
        }
    }

    /**
     * Confirmación de pedido (RF9). Se llama con la transacción ya confirmada, así
     * que este correo nunca puede hablar de un pedido que después se deshizo.
     */
    @Async
    public void sendOrderConfirmation(String to, String username, Long orderId, BigDecimal total,
            List<String> lines, Locale locale) {
        String order = String.valueOf(orderId);
        String body = String.join("\n\n",
                messages.getMessage("order.confirmation.greeting", new Object[] { username }, locale),
                messages.getMessage("order.confirmation.body",
                        new Object[] { order, formatTotal(total) }, locale),
                messages.getMessage("order.confirmation.items", null, locale)
                        + "\n" + String.join("\n", lines),
                messages.getMessage("order.confirmation.link", null, locale),
                orderLink(orderId, locale));

        send(to, messages.getMessage("order.confirmation.subject", new Object[] { order }, locale),
                body, "de confirmación del pedido " + order);
    }

    /** Aviso de cambio de estado (RF10), también fuera de la transacción. */
    @Async
    public void sendOrderStatusChanged(String to, String username, Long orderId,
            OrderStatus status, Locale locale) {
        String order = String.valueOf(orderId);
        // El estado se traduce aquí y solo aquí: la API sigue contestando "SHIPPED".
        String label = messages.getMessage("order.status." + status.name(), null, locale);
        String body = String.join("\n\n",
                messages.getMessage("order.confirmation.greeting", new Object[] { username }, locale),
                messages.getMessage("order.status.body", new Object[] { order, label }, locale));

        send(to, messages.getMessage("order.status.subject", new Object[] { order }, locale),
                body, "de estado del pedido " + order);
    }

    private void send(String to, String subject, String body, String what) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Correo {} enviado a {}", what, to);
        } catch (MailException ex) {
            // El pedido ya está guardado y respondido: un correo que no sale no lo
            // deshace. Aquí solo queda dejar rastro.
            log.error("No se pudo enviar el correo {} a {}", what, to, ex);
        }
    }

    private String orderLink(Long orderId, Locale locale) {
        return "%s/%s/orders/%d".formatted(frontendBaseUrl, locale.getLanguage(), orderId);
    }

    /**
     * {@code USD 120.00}. Prefijo fijo y no formato de moneda por idioma: el
     * catálogo no dice en qué divisa está, e inventarle un símbolo por locale
     * afirmaría algo que el dato no sostiene.
     */
    private static String formatTotal(BigDecimal total) {
        return "USD " + total.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

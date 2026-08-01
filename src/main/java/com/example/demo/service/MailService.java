package com.example.demo.service;

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

    public MailService(JavaMailSender mailSender, MessageSource messages,
            @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.messages = messages;
        this.from = from;
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
}

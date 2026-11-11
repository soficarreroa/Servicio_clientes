package com.empresa.notificaciones.service;

import com.empresa.notificaciones.dto.NotificacionRequest;
import com.empresa.notificaciones.dto.NotificacionResponse;
import com.empresa.notificaciones.model.NotificacionAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    @Value("${notificaciones.smtp.habilitado:false}")
    private boolean smtpEnabled;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    // Bitácora en memoria de auditoría de notificaciones (las últimas 100)
    private final Deque<NotificacionAudit> bitacora = new ConcurrentLinkedDeque<>();

    public NotificacionResponse procesarNotificacion(NotificacionRequest request) {
        String codigoSeguimiento = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime fecha = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String canal = smtpEnabled ? "SMTP" : "SIMULADO_LOG";
        String estadoEnvio = "ENVIADA";

        // 1. Simulación estructurada por Consola / Logs
        log.info("==========================================================================");
        log.info("[NOTIFICACION EMITIDA] Tracking: {}", codigoSeguimiento);
        log.info("  Canal:        {}", canal);
        log.info("  Tipo:         {}", request.tipo());
        log.info("  Cliente ID:   {}", request.clienteId());
        log.info("  Destinatario: {}", request.email());
        log.info("  Mensaje:      {}", request.mensaje());
        log.info("==========================================================================");

        // 2. Intento de envío SMTP si está configurado
        if (smtpEnabled && mailSender != null) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(request.email());
                mailMessage.setSubject("Aviso de su pedido - " + request.tipo());
                mailMessage.setText(request.mensaje());
                mailSender.send(mailMessage);
                log.info("Correo electrónico enviado con éxito a {}", request.email());
            } catch (Exception e) {
                log.warn("Fallo en el servidor SMTP al enviar a {}: {}. El flujo continúa de forma graceful.",
                        request.email(), e.getMessage());
                estadoEnvio = "ENVIADA_LOG_SIMULADO (FALLO_SMTP)";
            }
        }

        // 3. Registrar en bitácora de auditoría
        NotificacionAudit registro = new NotificacionAudit(
                codigoSeguimiento,
                request.clienteId(),
                request.email(),
                request.tipo(),
                request.mensaje(),
                fecha,
                canal,
                estadoEnvio
        );
        bitacora.addFirst(registro);
        if (bitacora.size() > 100) {
            bitacora.removeLast();
        }

        return new NotificacionResponse(
                codigoSeguimiento,
                estadoEnvio,
                "Notificación procesada y registrada exitosamente",
                fecha
        );
    }

    public List<NotificacionAudit> listarHistorial() {
        return new ArrayList<>(bitacora);
    }

    public Optional<NotificacionAudit> consultarPorCodigo(String codigoSeguimiento) {
        return bitacora.stream()
                .filter(n -> n.codigoSeguimiento().equalsIgnoreCase(codigoSeguimiento))
                .findFirst();
    }
}


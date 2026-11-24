package com.parking.notification.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.notification.model.ReservationEvent;
import com.parking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/** Consumes the reservation events published by the parking service. */
@Component
public class ReservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ReservationEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${notification.kafka.reservation-topic}")
    public void onReservationEvent(String payload, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        log.info("Received from partition {}: {}", partition, payload);
        try {
            notificationService.handle(objectMapper.readValue(payload, ReservationEvent.class));
        } catch (JsonProcessingException e) {
            log.error("Ignoring malformed reservation event: {}", payload, e);
        }
    }
}

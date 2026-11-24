package com.parking.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends reservation events to Kafka once the database transaction is committed,
 * so consumers never hear about a reservation that was rolled back.
 * A Kafka failure is logged and never cancels the reservation itself.
 */
@Slf4j
@Component
public class ReservationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public ReservationEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                     @Value("${parking.kafka.reservation-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ReservationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // Keyed by reservation id: all events of one reservation land on the same partition, in order.
            kafkaTemplate.send(topic, String.valueOf(event.reservationId()), payload)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Could not publish {} for reservation {}: {}", event.type(),
                                    event.reservationId(), error.getMessage());
                        } else {
                            log.info("Published {} for reservation {} on {}", event.type(), event.reservationId(),
                                    result.getRecordMetadata());
                        }
                    });
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Could not publish {} for reservation {}: {}", event.type(), event.reservationId(),
                    e.getMessage());
        }
    }
}

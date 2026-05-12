package com.payment.notification.kafka;

import com.payment.common.events.PaymentEvent;
import com.payment.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the notification-service.
 *
 * <p>Listens to all payment topics and delegates to {@link NotificationService}
 * for dispatching user-facing notifications.
 *
 * <p>Uses manual acknowledgment to ensure notifications are only confirmed
 * after successful processing. Notification failures (e.g., email delivery)
 * are caught internally and do not cause offset rollback.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = {
            "payment.initiated",
            "payment.processed",
            "payment.failed",
            "payment.completed"
        },
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Notification consumer received event [topic={}, partition={}, offset={}, eventId={}, paymentId={}]",
                topic, partition, offset, event.getEventId(), event.getPaymentId());

        try {
            notificationService.processEvent(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process notification event [eventId={}, paymentId={}]: {}",
                    event.getEventId(), event.getPaymentId(), e.getMessage(), e);
            // Re-throw to trigger the configured retry back-off
            throw e;
        }
    }
}

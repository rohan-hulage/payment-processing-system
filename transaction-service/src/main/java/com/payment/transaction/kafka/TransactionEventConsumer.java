package com.payment.transaction.kafka;

import com.payment.common.events.PaymentEvent;
import com.payment.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to all payment topics and creates transaction records.
 *
 * <p>Uses manual acknowledgment (MANUAL_IMMEDIATE) so offsets are only committed
 * after the transaction has been successfully persisted. This prevents data loss
 * if the service crashes mid-processing.
 *
 * <p>Consumer-side idempotency is enforced via the event ID unique constraint
 * in {@link TransactionService#createFromEvent}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final TransactionService transactionService;

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

        log.info("Received payment event [topic={}, partition={}, offset={}, eventId={}, paymentId={}, status={}]",
                topic, partition, offset, event.getEventId(), event.getPaymentId(), event.getStatus());

        try {
            transactionService.createFromEvent(event);
            acknowledgment.acknowledge();
            log.debug("Acknowledged event [eventId={}]", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process payment event [eventId={}, paymentId={}]: {}",
                    event.getEventId(), event.getPaymentId(), e.getMessage(), e);
            // Do NOT acknowledge — the error handler will retry
            throw e;
        }
    }
}

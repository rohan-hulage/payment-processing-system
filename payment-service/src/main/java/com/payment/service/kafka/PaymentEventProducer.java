package com.payment.service.kafka;

import com.payment.common.events.PaymentEvent;
import com.payment.service.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer that publishes payment lifecycle events to the appropriate topics.
 * Uses the payment ID as the message key to ensure ordering per payment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    /**
     * Publishes a payment event to the given topic.
     * The payment ID is used as the partition key for ordering guarantees.
     *
     * @param topic  the Kafka topic name
     * @param event  the payment event payload
     */
    public void publishEvent(String topic, PaymentEvent event) {
        CompletableFuture<SendResult<String, PaymentEvent>> future =
                kafkaTemplate.send(topic, event.getPaymentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                    "Failed to publish event [eventId={}, paymentId={}, topic={}]: {}",
                    event.getEventId(), event.getPaymentId(), topic, ex.getMessage(), ex
                );
            } else {
                log.info(
                    "Published event [eventId={}, paymentId={}, topic={}, partition={}, offset={}]",
                    event.getEventId(),
                    event.getPaymentId(),
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
                );
            }
        });
    }

    public void publishPaymentInitiated(PaymentEvent event) {
        event.setEventType(KafkaConfig.TOPIC_PAYMENT_INITIATED);
        publishEvent(KafkaConfig.TOPIC_PAYMENT_INITIATED, event);
    }

    public void publishPaymentProcessed(PaymentEvent event) {
        event.setEventType(KafkaConfig.TOPIC_PAYMENT_PROCESSED);
        publishEvent(KafkaConfig.TOPIC_PAYMENT_PROCESSED, event);
    }

    public void publishPaymentFailed(PaymentEvent event) {
        event.setEventType(KafkaConfig.TOPIC_PAYMENT_FAILED);
        publishEvent(KafkaConfig.TOPIC_PAYMENT_FAILED, event);
    }

    public void publishPaymentCompleted(PaymentEvent event) {
        event.setEventType(KafkaConfig.TOPIC_PAYMENT_COMPLETED);
        publishEvent(KafkaConfig.TOPIC_PAYMENT_COMPLETED, event);
    }
}

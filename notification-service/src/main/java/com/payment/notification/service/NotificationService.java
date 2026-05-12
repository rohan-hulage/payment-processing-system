package com.payment.notification.service;

import com.payment.common.enums.PaymentStatus;
import com.payment.common.events.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Notification service that dispatches alerts based on payment events.
 *
 * <p>Currently supports email notifications. The architecture is designed to
 * be extended with SMS (Twilio), push notifications, or webhooks by adding
 * additional send methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from:noreply@payment-system.com}")
    private String fromAddress;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Processes a payment event and dispatches the appropriate notification.
     *
     * @param event the payment event received from Kafka
     */
    public void processEvent(PaymentEvent event) {
        log.info("Processing notification for event [eventId={}, paymentId={}, status={}]",
                event.getEventId(), event.getPaymentId(), event.getStatus());

        switch (event.getStatus()) {
            case PENDING     -> notifyPaymentInitiated(event);
            case PROCESSING  -> notifyPaymentProcessing(event);
            case COMPLETED   -> notifyPaymentCompleted(event);
            case FAILED      -> notifyPaymentFailed(event);
            case REFUNDED    -> notifyPaymentRefunded(event);
            default          -> log.warn("No notification handler for status: {}", event.getStatus());
        }
    }

    private void notifyPaymentInitiated(PaymentEvent event) {
        String subject = "Payment Initiated - " + event.getPaymentId();
        String body = String.format(
            "Your payment of %s %s has been initiated.\n\nPayment ID: %s\nMethod: %s\n\nWe will notify you once it is processed.",
            event.getAmount(), event.getCurrency(),
            event.getPaymentId(),
            event.getPaymentMethod()
        );
        sendEmail(event.getPayerId(), subject, body);
    }

    private void notifyPaymentProcessing(PaymentEvent event) {
        String subject = "Payment Processing - " + event.getPaymentId();
        String body = String.format(
            "Your payment of %s %s is currently being processed.\n\nPayment ID: %s",
            event.getAmount(), event.getCurrency(), event.getPaymentId()
        );
        sendEmail(event.getPayerId(), subject, body);
    }

    private void notifyPaymentCompleted(PaymentEvent event) {
        // Notify payer
        String payerSubject = "Payment Successful - " + event.getPaymentId();
        String payerBody = String.format(
            "Your payment of %s %s has been successfully completed.\n\nPayment ID: %s\nPayee: %s\n\nThank you for using our payment system.",
            event.getAmount(), event.getCurrency(),
            event.getPaymentId(),
            event.getPayeeId()
        );
        sendEmail(event.getPayerId(), payerSubject, payerBody);

        // Notify payee
        String payeeSubject = "Payment Received - " + event.getPaymentId();
        String payeeBody = String.format(
            "You have received a payment of %s %s.\n\nPayment ID: %s\nFrom: %s",
            event.getAmount(), event.getCurrency(),
            event.getPaymentId(),
            event.getPayerId()
        );
        sendEmail(event.getPayeeId(), payeeSubject, payeeBody);
    }

    private void notifyPaymentFailed(PaymentEvent event) {
        String subject = "Payment Failed - " + event.getPaymentId();
        String body = String.format(
            "Unfortunately, your payment of %s %s has failed.\n\nPayment ID: %s\nReason: %s\n\nPlease try again or contact support.",
            event.getAmount(), event.getCurrency(),
            event.getPaymentId(),
            event.getFailureReason() != null ? event.getFailureReason() : "Unknown error"
        );
        sendEmail(event.getPayerId(), subject, body);
    }

    private void notifyPaymentRefunded(PaymentEvent event) {
        String subject = "Payment Refunded - " + event.getPaymentId();
        String body = String.format(
            "Your payment of %s %s has been refunded.\n\nPayment ID: %s\n\nThe refund will appear in your account within 3-5 business days.",
            event.getAmount(), event.getCurrency(), event.getPaymentId()
        );
        sendEmail(event.getPayerId(), subject, body);
    }

    /**
     * Sends an email notification.
     * In production, the recipient address would be looked up from a user service.
     * Here we log the notification and send if email is enabled.
     *
     * @param recipientId the user ID of the recipient
     * @param subject     the email subject
     * @param body        the email body
     */
    private void sendEmail(String recipientId, String subject, String body) {
        log.info("Sending email notification [to={}, subject={}]", recipientId, subject);

        if (!emailEnabled) {
            log.debug("Email notifications disabled. Would have sent: subject='{}' to='{}'", subject, recipientId);
            return;
        }

        try {
            // In production, resolve recipientId to an actual email address via user service
            // For now, treat recipientId as the email address if it contains '@'
            String toAddress = recipientId.contains("@") ? recipientId : recipientId + "@example.com";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toAddress);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            log.info("Email sent successfully to: {}", toAddress);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipientId, e.getMessage(), e);
            // Don't rethrow — notification failure should not block payment processing
        }
    }
}

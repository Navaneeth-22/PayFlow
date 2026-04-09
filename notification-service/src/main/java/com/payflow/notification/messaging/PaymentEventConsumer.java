package com.payflow.notification.messaging;

import com.payflow.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "payment.completed",
            groupId = "notification-completed-group"
    )
    public void onPaymentCompleted(Map<String, Object> event) {
            log.info("PAYMENT_COMPLETED received: paymentId={}",
                    event.get("paymentId"));
            notificationService.handlePaymentEvent(event, "PAYMENT_COMPLETED");
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "notification-failed-group"
    )
    public void onPaymentFailed(Map<String, Object> event) {
            log.info("PAYMENT_FAILED received: paymentId={}",
                    event.get("paymentId"));
            notificationService.handlePaymentEvent(event, "PAYMENT_FAILED");
    }
}
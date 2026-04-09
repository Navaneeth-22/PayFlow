package com.payflow.payment.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
@Slf4j
public class DltMonitor {

    @KafkaListener(
            topics = {
                    "fraud.cleared.DLT",
                    "fraud.flagged.DLT",
                    "ledger.debited.DLT",
                    "ledger.failed.DLT",
                    "ledger.reversed.DLT"
            },
            groupId = "payment-service-dlt-monitor"
    )
    public void onDeadLetter(ConsumerRecord<String, Object> record) {
        log.error(
                "MESSAGE IN DEAD LETTER TOPIC — REQUIRES INVESTIGATION: " +
                        "topic={} partition={} offset={} key={} value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value()
        );

    }
}
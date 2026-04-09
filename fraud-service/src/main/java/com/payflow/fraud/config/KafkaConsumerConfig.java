package com.payflow.fraud.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) -> {
                            log.error(
                                    "Sending to DLT: topic={} partition={} offset={} " +
                                            "key={} exception={}",
                                    record.topic(),
                                    record.partition(),
                                    record.offset(),
                                    record.key(),
                                    exception.getMessage()
                            );
                            return new TopicPartition(
                                    record.topic() + ".DLT",
                                    record.partition()
                            );
                        }
                );
        FixedBackOff backOff = new FixedBackOff(
                2000L,
                3L
        );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(
                NullPointerException.class,
                IllegalArgumentException.class,
                ClassCastException.class
        );

        return errorHandler;
    }
}
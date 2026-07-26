package ch.meldehub.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka altyapı tanımları.
 *
 * DLQ (Dead Letter Queue) politikası: consumer bir mesajı 2 denemeden
 * sonra hâlâ işleyemiyorsa, mesaj '<topic>.DLT' kuyruğuna taşınır.
 * Böylece zehirli mesaj (poison message) kuyruğu kilitlemez;
 * operasyon ekibi DLQ'ya bakıp müdahale eder.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic caseCreatedTopic(@Value("${app.kafka.case-created-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<String, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaOperations,
                        (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}

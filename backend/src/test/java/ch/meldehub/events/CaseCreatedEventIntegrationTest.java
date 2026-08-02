package ch.meldehub.events;

import ch.meldehub.domain.Case;
import ch.meldehub.domain.CaseCategory;
import ch.meldehub.service.CaseService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Event'in topic'e GERÇEKTEN basıldığının kanıtı.
 * @EmbeddedKafka: test içinde gerçek (gömülü) Kafka broker kalkar —
 * mock yok; producer → broker → consumer tam yolculuk ölçülür.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"case-created"})
class CaseCreatedEventIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private CaseService caseService;

    // CASE-252: create() artık Kafka'ya senkron basmaz; event outbox'a yazılır,
    // relay asenkron basar. Testte relay ELLE tetiklenir (scheduler testte fiilen
    // kapalı — bkz. test application.yml, relay-delay-ms: 600000).
    @Autowired
    private OutboxRelay outboxRelay;

    @Test
    void vakaYaratilincaCaseCreatedEventiTopicteOlur() {
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("event-dogrulama", "true", broker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "ch.meldehub.events");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "ch.meldehub.events.CaseCreatedEvent");

        try (Consumer<String, CaseCreatedEvent> consumer =
                     new DefaultKafkaConsumerFactory<String, CaseCreatedEvent>(consumerProps)
                             .createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "case-created");

            Case created = caseService.create("Çöp kutusu taşmış", "Park yanındaki kutu dolu",
                    CaseCategory.WASTE, "Seepromenade 4", "vatandas@example.ch");
            outboxRelay.publishPending();   // outbox → Kafka, senkron ack ile

            ConsumerRecords<String, CaseCreatedEvent> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

            // Paylaşılan gömülü broker'da başka testlerin event'leri de bulunabilir;
            // ilk kayda değil, BU TESTİN vakasına ait kayda bak (order-independent).
            List<ConsumerRecord<String, CaseCreatedEvent>> all = new java.util.ArrayList<>();
            records.forEach(all::add);
            assertThat(all).anySatisfy(record -> {
                assertThat(record.key()).isEqualTo(created.getId().toString());
                assertThat(record.value().caseId()).isEqualTo(created.getId());
                assertThat(record.value().category()).isEqualTo(CaseCategory.WASTE);
                assertThat(record.value().location()).isEqualTo("Seepromenade 4");
            });
        }
    }
}

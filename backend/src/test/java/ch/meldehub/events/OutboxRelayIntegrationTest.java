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
 * Transactional Outbox'un uçtan uca kanıtı (CASE-252).
 *
 * (1) create() vaka + outbox satırını TEK transaction'da yazar → dual-write
 * kaybı imkânsız: vaka varsa yayınlanmamış outbox satırı da vardır.
 * (2) Relay, bekleyen satırı Kafka'ya basar, broker ack'inden SONRA satırı
 * published işaretler; event topic'e caseId key'i ile ulaşır.
 * Scheduler testte fiilen kapalıdır (relay-delay-ms: 600000) — relay elle tetiklenir.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"case-created"})
class OutboxRelayIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private CaseService caseService;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void vakaYaratilincaAtomikOlarakOutboxSatiriDaOlusur() {
        outboxEventRepository.deleteAll();   // izolasyon: sadece bu testin satırını ölç

        Case created = caseService.create("Sokak lambası yanmıyor", "3 gündür karanlık",
                CaseCategory.LIGHTING, "Bahnhofstrasse 10", "vatandas@example.ch");

        List<OutboxEvent> rows = outboxEventRepository.findAll();
        assertThat(rows).hasSize(1);
        OutboxEvent row = rows.get(0);
        assertThat(row.isPublished()).isFalse();
        assertThat(row.getPublishedAt()).isNull();
        assertThat(row.getEventType()).isEqualTo("CaseCreated");
        assertThat(row.getAggregateId()).isEqualTo(created.getId());
        assertThat(row.getPayload()).contains("LIGHTING");
        assertThat(row.getCreatedAt()).isNotNull();
    }

    @Test
    void relayBekleyenEventiTopiceBasarVeSatiriPublishedIsaretler() {
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("outbox-dogrulama", "true", broker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "ch.meldehub.events");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "ch.meldehub.events.CaseCreatedEvent");

        try (Consumer<String, CaseCreatedEvent> consumer =
                     new DefaultKafkaConsumerFactory<String, CaseCreatedEvent>(consumerProps)
                             .createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "case-created");

            outboxEventRepository.deleteAll();
            Case created = caseService.create("Çöp kutusu taşmış", "Park yanındaki kutu dolu",
                    CaseCategory.WASTE, "Seepromenade 4", "vatandas@example.ch");

            outboxRelay.publishPending();

            // Satır, ancak broker ack'inden sonra published olur (at-least-once)
            OutboxEvent row = outboxEventRepository.findAll().get(0);
            assertThat(row.isPublished()).isTrue();
            assertThat(row.getPublishedAt()).isNotNull();

            // Event topic'e ulaştı, key = caseId string'i (vaka bazında sıra garantisi)
            ConsumerRecords<String, CaseCreatedEvent> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            ConsumerRecord<String, CaseCreatedEvent> record = records.iterator().next();
            assertThat(record.key()).isEqualTo(created.getId().toString());
            assertThat(record.value().caseId()).isEqualTo(created.getId());
            assertThat(record.value().category()).isEqualTo(CaseCategory.WASTE);
        }
    }
}

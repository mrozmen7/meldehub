package ch.meldehub.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Outbox relay — outbox tablosundaki bekleyen satırları Kafka'ya basan zamanlanmış süreç
 * (CASE-252, ADR-0008 güncellemesi).
 *
 * Rolü: {@code CaseService} vakayı + outbox satırını TEK transaction'da yazar (atomik);
 * bu relay o satırları pollayıp topic'e basar ve ancak başarılı olanları "published" işaretler.
 * Böylece Kafka kesintisi artık event kaybı demek değildir — satır tabloda bekler,
 * sonraki tick'te yeniden denenir.
 *
 * Neden senkron .get()? Satırı "published" işaretlemeden ÖNCE broker onayını (ack)
 * beklemek zorunluyuz. Asenkron basıp hemen işaretlersek, ack gelmeden uygulama
 * çökerse satır published kalır ama mesaj gitmemiş olur — tam da çözmek istediğimiz
 * kayıp. .get(10 sn) ack'i bloklayarak bekler; ack = en-az-bir-kez (at-least-once) garantisi.
 *
 * Neden hatada TÜM batch iptal? Bir event başarısız olursa exception fırlatılır,
 * transaction rollback olur ve hiçbir satır published işaretlenmez (öncekiler dahil).
 * Böylece sıralama bozulmaz ve hiçbir satır yanlışlıkla "gitti" sayılmaz;
 * sonraki tick aynı batch'i baştan dener.
 *
 * Duplicate olabilir: ack alındıktan sonra ama commit'ten önce çökme olursa event
 * yeniden basılır → consumer IDEMPOTENT olmalıdır (aynı caseId'yi ikinci kez
 * görmeye dayanıklı). Bu, at-least-once'un doğal bedelidir; bkz. ADR-0008 / CASE-252.
 *
 * (Eski CaseEventProducer'dan taşınan not: key = caseId — aynı vakanın event'leri
 * aynı partition'a düşer → vaka bazında sıra garantisi korunur.)
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String caseCreatedTopic;

    public OutboxRelay(OutboxEventRepository outboxEventRepository,
                       KafkaTemplate<String, Object> kafkaTemplate,
                       ObjectMapper objectMapper,
                       @Value("${app.kafka.case-created-topic}") String caseCreatedTopic) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.caseCreatedTopic = caseCreatedTopic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-delay-ms:5000}",
            initialDelayString = "${app.outbox.relay-delay-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent outboxEvent : pending) {
            try {
                CaseCreatedEvent caseCreatedEvent =
                        objectMapper.readValue(outboxEvent.getPayload(), CaseCreatedEvent.class);
                // Key = caseId (aggregateId): vaka bazında partition/sıra garantisi.
                // .get() = ack'i bekle: satır ancak broker onayından SONRA published olur.
                kafkaTemplate.send(caseCreatedTopic,
                        outboxEvent.getAggregateId().toString(), caseCreatedEvent)
                        .get(10, TimeUnit.SECONDS);
                outboxEvent.markPublished(Instant.now());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Outbox relay kesildi (event {}): batch iptal, sonraki tick yeniden dener",
                        outboxEvent.getId());
                throw new IllegalStateException("Outbox relay kesintiye uğradı", ie);
            } catch (Exception ex) {
                // Tek event hatası = TÜM batch rollback: sıra korunur, yanlış "published" yok.
                log.error("Outbox event'i yayınlanamadı ({}): batch iptal, sonraki tick yeniden dener — {}",
                        outboxEvent.getId(), ex.getMessage());
                throw new IllegalStateException("Outbox relay hatası", ex);
            }
        }
    }
}

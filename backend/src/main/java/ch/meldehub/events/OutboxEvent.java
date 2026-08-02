package ch.meldehub.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox satırı — Transactional Outbox pattern'in kalbi (CASE-252, ADR-0008 güncellemesi).
 *
 * Bu tablo şu problemi çözer: vaka kaydı (cases) ile event bildirimi (Kafka)
 * iki ayrı sisteme yazılır; arada biri başarısız olursa veri tutarsız kalır
 * ("dual-write" sorunu — ADR-0006'da bilinçli borç olarak kayıtlıydı).
 * Çözüm: vaka ve outbox satırı AYNI veritabanı transaction'ında yazılır,
 * ikisi birden ya commit olur ya rollback — event kaybı imkânsızlaşır.
 * Kafka'ya basım bu tablodaki satırlardan {@link OutboxRelay} ile asenkron yapılır.
 *
 * payload, event'in Jackson ile serialize edilmiş JSON halidir (text kolon —
 * jsonb değil: H2 test uyumu + ddl-auto:validate güvenliği).
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Event'in ait olduğu aggregate'in kimliği (ör. vaka id'si) — Kafka key'i de budur. */
    @Column(nullable = false)
    private UUID aggregateId;

    /** Event sözleşmesinin adı (ör. "CaseCreated") — consumer yönlendirmesi için etiket. */
    @Column(nullable = false, length = 100)
    private String eventType;

    /** Event'in JSON gövdesi — text kolon (jsonb değil; bkz. sınıf javadoc'u). */
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    /** Relay, broker onayını (ack) aldıktan sonra TRUE yapar — önce asla. */
    @Column(nullable = false)
    private boolean published;

    @Column
    private Instant publishedAt;

    protected OutboxEvent() {
        // JPA için zorunlu parametresiz kurucu
    }

    private OutboxEvent(UUID aggregateId, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.published = false;   // her outbox satırı "yayınlanmamış" doğar
    }

    /**
     * Yeni yayınlanmamış outbox satırı üretir. Factory kullanılır çünkü
     * "yeni satır = published:false + createdAt:şimdi" kuralı tek yerde dursun;
     * çağıranın bu alanları elle doğru kurmasına güvenilmez.
     */
    public static OutboxEvent of(UUID aggregateId, String eventType, String payload) {
        return new OutboxEvent(aggregateId, eventType, payload);
    }

    /** Relay, Kafka ack aldıktan sonra çağırır — yayın damgası ancak o zaman düşülür. */
    public void markPublished(Instant publishedAt) {
        this.published = true;
        this.publishedAt = publishedAt;
    }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isPublished() { return published; }
    public Instant getPublishedAt() { return publishedAt; }
}

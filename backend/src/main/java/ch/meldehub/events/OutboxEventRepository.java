package ch.meldehub.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Outbox satırlarına erişim. Relay'in tek sorgusu: yayınlanmamış ilk 50 satır,
 * oluşum sırasına göre — batch sınırı (50) tek tick'in sonsuz sürmesini engeller;
 * createdAt ASC sıralaması + V3'teki partial index (idx_outbox_unpublished) sayesinde
 * sorgu tablo büyüse bile ucuz kalır ve event sırası korunur.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}

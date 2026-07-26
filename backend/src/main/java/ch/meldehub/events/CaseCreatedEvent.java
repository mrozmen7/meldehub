package ch.meldehub.events;

import ch.meldehub.domain.CaseCategory;

import java.time.Instant;
import java.util.UUID;

/**
 * case-created event'i — topic'e basılan sözleşme.
 *
 * Entity BASILMAZ: event sözleşmesi ayrı DTO'dur; domain iç yapısı
 * değişse bile topic'teki format sabit kalır (consumer'ları kırmayız).
 * Alan eklemek geriye uyumlu, alan silmek/değiştirmek uyumsuzdur —
 * bu yüzden sözleşme minimal tutulur.
 */
public record CaseCreatedEvent(
        UUID caseId,
        CaseCategory category,
        String location,
        Instant occurredAt
) {
}

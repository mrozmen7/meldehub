package ch.meldehub.api;

import ch.meldehub.domain.Case;
import ch.meldehub.domain.CaseCategory;
import ch.meldehub.domain.CaseStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Vaka cevabı (DTO) — dışarıya döndüğümüz görünüm.
 * Entity'den farkı: ileride iç alanlar (ör. notlar) eklense bile
 * API sözleşmesi sabit kalır.
 */
public record CaseResponse(
        UUID id,
        String title,
        String description,
        CaseCategory category,
        CaseStatus status,
        String location,
        String reporterEmail,
        Instant createdAt,
        Instant updatedAt
) {
    public static CaseResponse from(Case c) {
        return new CaseResponse(
                c.getId(), c.getTitle(), c.getDescription(), c.getCategory(),
                c.getStatus(), c.getLocation(), c.getReporterEmail(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}

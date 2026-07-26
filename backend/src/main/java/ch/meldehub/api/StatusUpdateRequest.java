package ch.meldehub.api;

import ch.meldehub.domain.CaseStatus;
import jakarta.validation.constraints.NotNull;

/** Durum güncelleme isteği (DTO). */
public record StatusUpdateRequest(
        @NotNull CaseStatus status
) {
}

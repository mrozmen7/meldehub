package ch.meldehub.api;

import ch.meldehub.domain.CaseCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Vaka oluşturma isteği (DTO) — dış dünyadan gelen veri.
 * Entity'yi dışarı açmayız; giriş/çıkış DTO ile olur.
 * Validasyon anotasyonları controller'da @Valid ile devreye girer.
 */
public record CaseCreateRequest(
        @NotBlank String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull CaseCategory category,
        @NotBlank String location,
        @NotBlank @Email String reporterEmail
) {
}

package ch.meldehub.api;

import jakarta.validation.constraints.NotBlank;

/** POST /api/auth/login istek gövdesi (CASE-201). */
public record LoginRequest(
        @NotBlank(message = "kullanıcı adı zorunludur") String username,
        @NotBlank(message = "şifre zorunludur") String password) {
}

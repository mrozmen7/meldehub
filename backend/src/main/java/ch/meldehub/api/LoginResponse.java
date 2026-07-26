package ch.meldehub.api;

/** Başarılı login cevabı: Bearer token + kullanıcı bilgisi (CASE-201). */
public record LoginResponse(String token, String username, String role) {
}

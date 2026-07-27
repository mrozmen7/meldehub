package ch.meldehub.api;

import ch.meldehub.domain.InvalidStatusTransitionException;
import ch.meldehub.service.CaseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Merkezi hata çevirmeni — exception'ları HTTP cevaplarına döker.
 * Controller'lar try/catch ile kirlenmez; hata biçimi tüm API'de tutarlı:
 *   404 vaka yok | 409 geçersiz durum geçişi | 400 validasyon hatası
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CaseNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(CaseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ResponseEntity<Map<String, String>> conflict(InvalidStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("error", details));
    }

    /**
     * CASE-233: query param dönüşüm hatası (ör. ?status=BILINMEYEN) → 400.
     * Aksi hâlde Spring 500 dönerdi; API sözleşmesi tutarlı kalsın: {"error": "..."}.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, String>> typeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Geçersiz parametre değeri: " + ex.getName() + "=" + ex.getValue();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}

package ch.meldehub.api;

import ch.meldehub.domain.Case;
import ch.meldehub.domain.CaseStatus;
import ch.meldehub.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Vaka REST API'si — ince (thin) controller:
 * validasyon @Valid ile, iş mantığı serviste, kural entity'de.
 * Controller sadece HTTP ↔ servis çevirisidir.
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService service;

    public CaseController(CaseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CaseResponse> create(@Valid @RequestBody CaseCreateRequest request) {
        Case created = service.create(
                request.title(), request.description(), request.category(),
                request.location(), request.reporterEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(CaseResponse.from(created));
    }

    /**
     * CASE-233: sayfalı listeleme — cevap tipi Spring Data Page JSON'u
     * (content, totalElements, totalPages, number, size ...).
     *
     *   GET /api/cases?page=0&size=20&status=NEW
     *
     * status opsiyonel: verilirse CaseStatus değeri olmalı (geçersizse 400 —
     * GlobalExceptionHandler'daki MethodArgumentTypeMismatchException yakalayıcısı).
     */
    @GetMapping
    public Page<CaseResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) CaseStatus status) {
        return service.findAll(page, size, status).map(CaseResponse::from);
    }

    @GetMapping("/{id}")
    public CaseResponse findById(@PathVariable UUID id) {
        return CaseResponse.from(service.findById(id));
    }

    @PatchMapping("/{id}/status")
    public CaseResponse updateStatus(@PathVariable UUID id,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        return CaseResponse.from(service.updateStatus(id, request.status()));
    }
}

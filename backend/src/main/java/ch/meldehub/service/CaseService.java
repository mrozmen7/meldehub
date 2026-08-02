package ch.meldehub.service;

import ch.meldehub.domain.Case;
import ch.meldehub.domain.CaseCategory;
import ch.meldehub.domain.CaseRepository;
import ch.meldehub.domain.CaseStatus;
import ch.meldehub.events.CaseCreatedEvent;
import ch.meldehub.events.OutboxEvent;
import ch.meldehub.events.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * İş mantığı katmanı — controller ile veritabanı arasındaki tek kapı.
 *
 * @Transactional: her metot ya hep beraber başarır ya hiç (atomiklik).
 * Durum geçişi kuralı burada DEĞİL, entity'de (Case.changeStatus) —
 * servis sadece akışı yönetir, kuralı domain korur.
 *
 * CASE-252: Transactional Outbox uygulandı (ADR-0008 güncellemesi). Vaka kaydı
 * ile outbox satırı AYNI transaction'da yazılır → dual-write riski (Kafka
 * erişilemezken event'in sessizce kaybolması, eski ADR-0006 borcu) ortadan
 * kalktı. Kafka'ya basım OutboxRelay tarafından asenkron yapılır.
 */
@Service
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private final CaseRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public CaseService(CaseRepository repository,
                       OutboxEventRepository outboxEventRepository,
                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Case create(String title, String description, CaseCategory category,
                       String location, String reporterEmail) {
        Case newCase = new Case(title, description, category, location, reporterEmail);
        Case saved = repository.save(newCase);
        // Outbox: event artık Kafka'ya DEĞİL, aynı transaction'da outbox tablosuna yazılır.
        // Serialize hatası try/catch'SİZ yayılır → transaction rollback → fail fast:
        // ya iki satır birden kaydedilir ya hiçbiri (tutarlılık).
        String payload;
        try {
            payload = objectMapper.writeValueAsString(new CaseCreatedEvent(
                    saved.getId(), saved.getCategory(), saved.getLocation(), saved.getCreatedAt()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("case-created event'i serialize edilemedi", ex);
        }
        outboxEventRepository.save(OutboxEvent.of(saved.getId(), "CaseCreated", payload));
        log.debug("case-created event'i outbox'a yazıldı (vaka: {})", saved.getId());
        return saved;
    }

    /**
     * CASE-233: sayfalı listeleme. Sıralama sabit: createdAt DESC (en yeni üstte).
     * status null ise tüm vakalar, değilse sadece o durumdakiler döner.
     * size 100 ile sınırlanır (kötü niyetli/yanlışlıkla dev sayfalara karşı).
     */
    @Transactional(readOnly = true)
    public Page<Case> findAll(int page, int size, CaseStatus status) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Case findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new CaseNotFoundException(id));
    }

    @Transactional
    public Case updateStatus(UUID id, CaseStatus next) {
        Case existing = findById(id);
        existing.changeStatus(next);   // geçersizse entity fırlatır → 409
        return repository.save(existing);
    }
}

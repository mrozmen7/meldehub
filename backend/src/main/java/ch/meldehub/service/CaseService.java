package ch.meldehub.service;

import ch.meldehub.domain.Case;
import ch.meldehub.domain.CaseCategory;
import ch.meldehub.domain.CaseRepository;
import ch.meldehub.domain.CaseStatus;
import ch.meldehub.events.CaseEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * İş mantığı katmanı — controller ile veritabanı arasındaki tek kapı.
 *
 * @Transactional: her metot ya hep beraber başarır ya hiç (atomiklik).
 * Durum geçişi kuralı burada DEĞİL, entity'de (Case.changeStatus) —
 * servis sadece akışı yönetir, kuralı domain korur.
 *
 * Vaka kaydından sonra case-created event'i basılır. Kafka erişilemezse
 * istek PATLAMAZ: hata loglanır, vaka kaydedilmiş kalır (ADR-0006;
 * kalıcı çözüm Outbox pattern — Faz 11 backlog).
 */
@Service
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private final CaseRepository repository;
    private final CaseEventProducer eventProducer;

    public CaseService(CaseRepository repository, CaseEventProducer eventProducer) {
        this.repository = repository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public Case create(String title, String description, CaseCategory category,
                       String location, String reporterEmail) {
        Case newCase = new Case(title, description, category, location, reporterEmail);
        Case saved = repository.save(newCase);
        try {
            eventProducer.publishCaseCreated(saved);
        } catch (Exception ex) {
            log.error("case-created event'i basılamadı (vaka kayıtlı: {}): {}",
                    saved.getId(), ex.getMessage());
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Case> findAll() {
        return repository.findAll();
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

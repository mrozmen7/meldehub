package ch.meldehub.service;

import ch.meldehub.domain.Case;
import ch.meldehub.domain.CaseCategory;
import ch.meldehub.domain.CaseRepository;
import ch.meldehub.domain.CaseStatus;
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
 */
@Service
public class CaseService {

    private final CaseRepository repository;

    public CaseService(CaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Case create(String title, String description, CaseCategory category,
                       String location, String reporterEmail) {
        Case newCase = new Case(title, description, category, location, reporterEmail);
        return repository.save(newCase);
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

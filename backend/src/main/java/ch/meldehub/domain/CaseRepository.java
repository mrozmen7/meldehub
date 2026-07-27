package ch.meldehub.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository — arayüz yeterli, implementasyonu
 * Spring çalışma zamanında üretir. SQL yazmayız.
 *
 * CASE-233: sayfalı sorgular. findAll(Pageable) JpaRepository'den gelir;
 * durum filtresi için türetilmiş sorgu yeterli.
 */
public interface CaseRepository extends JpaRepository<Case, UUID> {

    Page<Case> findByStatus(CaseStatus status, Pageable pageable);
}

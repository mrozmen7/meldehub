package ch.meldehub.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository — arayüz yeterli, implementasyonu
 * Spring çalışma zamanında üretir. SQL yazmayız.
 */
public interface CaseRepository extends JpaRepository<Case, UUID> {
}

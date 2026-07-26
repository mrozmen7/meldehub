package ch.meldehub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Vaka (Case) — vatandaş ihbarının sisteme düşmüş hali.
 *
 * Zengin domain modeli: durum geçişi kuralı (changeStatus) entity'nin
 * kendisindedir; geçersiz geçiş InvalidStatusTransitionException fırlatır.
 * Böylece kural, hangi katman çağırırsa çağırsın atlatılamaz.
 */
@Entity
@Table(name = "cases")
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String reporterEmail;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Case() {
        // JPA için zorunlu parametresiz kurucu
    }

    public Case(String title, String description, CaseCategory category,
                String location, String reporterEmail) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.reporterEmail = reporterEmail;
        this.status = CaseStatus.NEW;   // her vaka NEW doğar
    }

    public void changeStatus(CaseStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(status, next);
        }
        this.status = next;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public CaseCategory getCategory() { return category; }
    public CaseStatus getStatus() { return status; }
    public String getLocation() { return location; }
    public String getReporterEmail() { return reporterEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

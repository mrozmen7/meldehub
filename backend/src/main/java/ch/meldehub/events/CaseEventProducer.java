package ch.meldehub.events;

import ch.meldehub.domain.Case;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Event üretici (producer) — tek sorumluluğu domain olayını topic'e basmak.
 *
 * Key = caseId: aynı vakanın event'leri aynı partition'a düşer → sıra garantisi.
 * Topic adı config'den gelir, koda gömülmez.
 */
@Component
public class CaseEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String caseCreatedTopic;

    public CaseEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                             @Value("${app.kafka.case-created-topic}") String caseCreatedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.caseCreatedTopic = caseCreatedTopic;
    }

    public void publishCaseCreated(Case createdCase) {
        CaseCreatedEvent event = new CaseCreatedEvent(
                createdCase.getId(),
                createdCase.getCategory(),
                createdCase.getLocation(),
                createdCase.getCreatedAt());
        kafkaTemplate.send(caseCreatedTopic, createdCase.getId().toString(), event);
    }
}

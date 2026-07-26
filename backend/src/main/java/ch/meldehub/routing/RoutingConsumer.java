package ch.meldehub.routing;

import ch.meldehub.events.CaseCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Yönlendirme tüketicisi (consumer) — case-created event'ini dinler,
 * kategoriyi birime eşler.
 *
 * Şimdilik kararı loglar (Sprint 14 kapsamı). Sonraki sprintte
 * vaka kaydına 'department' alanı yazacak veya bildirim servisini
 * tetikleyecek — event sözleşmesi değişmeden genişleyebilir.
 */
@Component
public class RoutingConsumer {

    private static final Logger log = LoggerFactory.getLogger(RoutingConsumer.class);

    @KafkaListener(topics = "${app.kafka.case-created-topic}", groupId = "meldehub-routing")
    public void onCaseCreated(CaseCreatedEvent event) {
        Department department = Department.fromCategory(event.category());
        log.info("Vaka {} kategorisi {} → {} birimine yönlendirildi",
                event.caseId(), event.category(), department);
    }
}

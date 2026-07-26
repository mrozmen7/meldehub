package ch.meldehub.domain;

/**
 * Vaka yaşam döngüsü — durum makinesi (state machine).
 *
 * NEW → TRIAGED → IN_PROGRESS → RESOLVED → CLOSED
 *
 * Kural KOD'da durur: geçersiz geçiş derhal reddedilir.
 * "İş kuralı nerede yaşamalı?" sorusunun cevabı: serviste değil,
 * domain nesnesinin kendisinde (zengin domain modeli).
 */
public enum CaseStatus {
    NEW, TRIAGED, IN_PROGRESS, RESOLVED, CLOSED;

    public boolean canTransitionTo(CaseStatus next) {
        return switch (this) {
            case NEW -> next == TRIAGED;
            case TRIAGED -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == RESOLVED;
            case RESOLVED -> next == CLOSED;
            case CLOSED -> false;   // kapanan vaka bir daha açılmaz
        };
    }
}

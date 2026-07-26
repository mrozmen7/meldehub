package ch.meldehub.domain;

/** Geçersiz durum geçişi denemesi → HTTP 409 Conflict. */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(CaseStatus from, CaseStatus to) {
        super("Geçersiz durum geçişi: " + from + " → " + to);
    }
}

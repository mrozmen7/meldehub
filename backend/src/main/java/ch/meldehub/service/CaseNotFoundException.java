package ch.meldehub.service;

import java.util.UUID;

/** Vaka yok → HTTP 404. */
public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(UUID id) {
        super("Vaka bulunamadı: " + id);
    }
}

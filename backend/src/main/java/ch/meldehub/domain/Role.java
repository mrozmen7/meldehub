package ch.meldehub.domain;

/**
 * Kullanıcı rolleri (CASE-201, ADR-0009).
 *
 * CITIZEN : vatandaş — ihbar verebilir (POST /api/cases).
 * OPERATOR: belediye operatörü — vakaları görür ve durumlarını yönetir
 *           (GET /api/cases/**, PATCH /api/cases/{id}/status); ihbar da verebilir.
 *
 * Enum + DB'de string olarak saklanır: yeni rol eklemek kod değişikliği ister
 * (bilinçli — rol seti kapalı ve küçük), ama kullanıcı bazında rol ataması
 * veri değişikliğidir, deploy gerektirmez.
 */
public enum Role {
    CITIZEN,
    OPERATOR
}

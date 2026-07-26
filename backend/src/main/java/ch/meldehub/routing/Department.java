package ch.meldehub.routing;

import ch.meldehub.domain.CaseCategory;

/**
 * Belediye birimleri — kategori hangi birime gider eşlemesi.
 * İş kuralı tek yerde: yarın notification servisi de aynı eşlemeyi kullanır.
 */
public enum Department {
    ROADS,           // yol işleri
    INFRASTRUCTURE,  // altyapı
    SANITATION,      // temizlik
    PUBLIC_ORDER,    // asayiş/düzen
    GENERAL;         // genel evrak

    public static Department fromCategory(CaseCategory category) {
        return switch (category) {
            case POTHOLE -> ROADS;
            case LIGHTING -> INFRASTRUCTURE;
            case WASTE -> SANITATION;
            case NOISE -> PUBLIC_ORDER;
            case OTHER -> GENERAL;
        };
    }
}

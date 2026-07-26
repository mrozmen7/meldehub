package ch.meldehub.routing;

import ch.meldehub.domain.CaseCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Kategori → birim eşlemesi: beş kategorinin beşi de doğru birime. */
class DepartmentTest {

    @Test
    void tumKategorilerDogruBirimeEsler() {
        assertThat(Department.fromCategory(CaseCategory.POTHOLE)).isEqualTo(Department.ROADS);
        assertThat(Department.fromCategory(CaseCategory.LIGHTING)).isEqualTo(Department.INFRASTRUCTURE);
        assertThat(Department.fromCategory(CaseCategory.WASTE)).isEqualTo(Department.SANITATION);
        assertThat(Department.fromCategory(CaseCategory.NOISE)).isEqualTo(Department.PUBLIC_ORDER);
        assertThat(Department.fromCategory(CaseCategory.OTHER)).isEqualTo(Department.GENERAL);
    }
}

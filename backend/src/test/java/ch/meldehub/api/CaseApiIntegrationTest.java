package ch.meldehub.api;

import ch.meldehub.config.JwtService;
import ch.meldehub.domain.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uçtan uca API testi — gerçek servis + gerçek repository (H2 ile).
 * Katmanları mock'lamayız; amaç: HTTP'den veritabanına tam yolculuk.
 *
 * CASE-201 sonrası tüm istekler GERÇEK JWT taşır: token'lar JwtService ile
 * üretilir, gerçek güvenlik filtre zinciri (JWT filter + yetki kuralları)
 * her istekte işler. Rol kuralları: POST → CITIZEN/OPERATOR, GET/PATCH → OPERATOR.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CaseApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private JwtService jwtService;

    private String operatorAuth() {
        return "Bearer " + jwtService.generateToken("operator", Role.OPERATOR);
    }

    private String citizenAuth() {
        return "Bearer " + jwtService.generateToken("citizen", Role.CITIZEN);
    }

    private Map<String, String> ornekVaka(String baslik) {
        return Map.of(
                "title", baslik,
                "description", "Kaldırımda büyük çukur var",
                "category", "POTHOLE",
                "location", "Bahnhofstrasse 1, Zürich",
                "reporterEmail", "vatandas@example.ch");
    }

    private String vakaYarat(String baslik) throws Exception {
        String cevap = mvc.perform(post("/api/cases")
                        .header("Authorization", citizenAuth())   // vatandaş ihbar verir
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ornekVaka(baslik))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(cevap).get("id").asText();
    }

    @Test
    void vakaYasamDongusuBasindanSonuna() throws Exception {
        String id = vakaYarat("Yol çukuru");

        // listeleme + tekil okuma (operatör yetkisi) — CASE-233: cevap artık Page JSON'u
        mvc.perform(get("/api/cases").header("Authorization", operatorAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").exists());
        mvc.perform(get("/api/cases/" + id).header("Authorization", operatorAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Yol çukuru"));

        // yaşam döngüsü: NEW → TRIAGED → IN_PROGRESS → RESOLVED → CLOSED
        for (String sonraki : new String[]{"TRIAGED", "IN_PROGRESS", "RESOLVED", "CLOSED"}) {
            mvc.perform(patch("/api/cases/" + id + "/status")
                            .header("Authorization", operatorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + sonraki + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(sonraki));
        }

        // CLOSED bir daha açılamaz → 409
        mvc.perform(patch("/api/cases/" + id + "/status")
                        .header("Authorization", operatorAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NEW\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void gecersizGecis409Doner() throws Exception {
        String id = vakaYarat("Aydınlatma arızası");

        // NEW → IN_PROGRESS atlanamaz → 409
        mvc.perform(patch("/api/cases/" + id + "/status")
                        .header("Authorization", operatorAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void bilinmeyenId404Doner() throws Exception {
        mvc.perform(get("/api/cases/" + UUID.randomUUID())
                        .header("Authorization", operatorAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void gecersizIstek400Doner() throws Exception {
        // boş başlık + bozuk e-posta → 400
        String bozuk = "{\"title\":\"\",\"description\":\"x\",\"category\":\"POTHOLE\","
                + "\"location\":\"y\",\"reporterEmail\":\"eposta-degil\"}";
        mvc.perform(post("/api/cases")
                        .header("Authorization", citizenAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bozuk))
                .andExpect(status().isBadRequest());
    }
}

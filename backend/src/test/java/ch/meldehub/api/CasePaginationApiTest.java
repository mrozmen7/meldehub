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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CASE-233 — sayfalama + durum filtresi uçtan uca testleri.
 *
 * Test bağlamı (ve H2) sınıflar arası paylaşıldığı için mutlak
 * totalElements yerine "önce/sonra farkı" doğrulanır:
 * önce mevcut toplam okunur, sonra N vaka eklenir, toplam N artmalı.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CasePaginationApiTest {

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

    private void vakaYarat(String baslik) throws Exception {
        Map<String, String> vaka = Map.of(
                "title", baslik,
                "description", "Sayfalama testi vakası",
                "category", "POTHOLE",
                "location", "Bahnhofstrasse 1, Zürich",
                "reporterEmail", "vatandas@example.ch");
        mvc.perform(post("/api/cases")
                        .header("Authorization", citizenAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(vaka)))
                .andExpect(status().isCreated());
    }

    private long toplamVakaSayisi() throws Exception {
        String cevap = mvc.perform(get("/api/cases?page=0&size=1")
                        .header("Authorization", operatorAuth()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(cevap).get("totalElements").asLong();
    }

    @Test
    void sayfaSifirBoyutIkiIleIkiKayitVeDogruToplamlarDoner() throws Exception {
        long oncekiToplam = toplamVakaSayisi();
        vakaYarat("Sayfalama vakası A");
        vakaYarat("Sayfalama vakası B");
        vakaYarat("Sayfalama vakası C");

        long yeniToplam = oncekiToplam + 3;
        long beklenenSayfaSayisi = (long) Math.ceil(yeniToplam / 2.0);

        mvc.perform(get("/api/cases?page=0&size=2")
                        .header("Authorization", operatorAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(yeniToplam))
                .andExpect(jsonPath("$.totalPages").value(beklenenSayfaSayisi))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                // createdAt DESC: ilk kayıt en yeni vaka olmalı
                .andExpect(jsonPath("$.content[0].title").value("Sayfalama vakası C"));
    }

    @Test
    void sizeUstu100eKirpilir() throws Exception {
        mvc.perform(get("/api/cases?page=0&size=500")
                        .header("Authorization", operatorAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void statusFiltresiSadeceODurumdakileriDoner() throws Exception {
        vakaYarat("NEW filtresi vakası");

        mvc.perform(get("/api/cases?status=NEW&size=100")
                        .header("Authorization", operatorAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("NEW"))))
                .andExpect(jsonPath("$.content[*].title",
                        org.hamcrest.Matchers.hasItem("NEW filtresi vakası")));
    }

    @Test
    void statusFiltresiToplamiDaFiltreler() throws Exception {
        // NEW toplamını oku, yeni bir NEW vaka ekle, toplam tam 1 artmalı
        String once = mvc.perform(get("/api/cases?status=NEW&page=0&size=1")
                        .header("Authorization", operatorAuth()))
                .andReturn().getResponse().getContentAsString();
        long oncekiNew = om.readTree(once).get("totalElements").asLong();

        vakaYarat("NEW toplam sayacı vakası");

        mvc.perform(get("/api/cases?status=NEW&page=0&size=1")
                        .header("Authorization", operatorAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(oncekiNew + 1));
    }

    @Test
    void gecersizStatus400Doner() throws Exception {
        mvc.perform(get("/api/cases?status=BILINMEYEN")
                        .header("Authorization", operatorAuth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void sayfaliListeTokensuz401Doner() throws Exception {
        mvc.perform(get("/api/cases?page=0&size=2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void varsayilanParametrelerleSayfaSifirBoyutYirmiDoner() throws Exception {
        mvc.perform(get("/api/cases")
                        .header("Authorization", operatorAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(0)));
    }
}

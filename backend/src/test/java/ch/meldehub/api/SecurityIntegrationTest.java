package ch.meldehub.api;

import ch.meldehub.config.JwtService;
import ch.meldehub.domain.AppUser;
import ch.meldehub.domain.AppUserRepository;
import ch.meldehub.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Güvenlik kurallarının uçtan uca kanıtı (CASE-201).
 *
 * Gerçek filtre zinciri test edilir (test profilinde security KAPALI değildir):
 * JWT filtresi → yetki kuralları → 401/403 hata biçimi.
 * Seed kullanıcılar H2'ye repository ile eklenir (Flyway testte kapalı — Faz 11 kararı;
 * V2 migration'ın gerçek doğrulaması docker-compose'taki PostgreSQL'de yapılır).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seedUsers() {
        if (users.findByUsername("citizen").isEmpty()) {
            users.save(new AppUser("citizen", passwordEncoder.encode("meldehub123"), Role.CITIZEN));
        }
        if (users.findByUsername("operator").isEmpty()) {
            users.save(new AppUser("operator", passwordEncoder.encode("meldehub123"), Role.OPERATOR));
        }
    }

    private String tokenFor(Role role) {
        return "Bearer " + jwtService.generateToken(role.name().toLowerCase(), role);
    }

    // ---------- Login ----------

    @Test
    void dogruKullaniciSifreIleLogin200VeTokenDoner() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operator\",\"password\":\"meldehub123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("operator"))
                .andExpect(jsonPath("$.role").value("OPERATOR"));
    }

    @Test
    void yanlisSifreIleLogin401Doner() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operator\",\"password\":\"yanlis-sifre\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Kullanıcı adı veya şifre hatalı"));
    }

    @Test
    void bilinmeyenKullaniciIleLogin401Doner() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"yok-boyle-biri\",\"password\":\"meldehub123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Kullanıcı adı veya şifre hatalı"));
    }

    // ---------- Yetki kuralları ----------

    @Test
    void tokensuzGetApiCases401Doner() throws Exception {
        mvc.perform(get("/api/cases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void bozukTokenIleGetApiCases401Doner() throws Exception {
        mvc.perform(get("/api/cases").header("Authorization", "Bearer bozuk.token.degeri"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void citizenTokenIlePatch403Doner() throws Exception {
        mvc.perform(patch("/api/cases/00000000-0000-0000-0000-000000000000/status")
                        .header("Authorization", tokenFor(Role.CITIZEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TRIAGED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void citizenTokenIleGetApiCases403Doner() throws Exception {
        mvc.perform(get("/api/cases").header("Authorization", tokenFor(Role.CITIZEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void operatorTokenIleGetApiCases200Doner() throws Exception {
        mvc.perform(get("/api/cases").header("Authorization", tokenFor(Role.OPERATOR)))
                .andExpect(status().isOk());
    }

    @Test
    void citizenTokenIleIhbarPost201Doner() throws Exception {
        String vaka = "{\"title\":\"Gürültü\",\"description\":\"Gece inşaat gürültüsü\","
                + "\"category\":\"NOISE\",\"location\":\"Langstrasse 10\","
                + "\"reporterEmail\":\"vatandas@example.ch\"}";
        mvc.perform(post("/api/cases")
                        .header("Authorization", tokenFor(Role.CITIZEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vaka))
                .andExpect(status().isCreated());
    }

    @Test
    void healthVeApiDocsAnonimErisilebilir() throws Exception {
        // Docker healthcheck /actuator/health'i tokensuz çağırır — açık kalmalı
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}

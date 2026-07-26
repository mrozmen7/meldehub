package ch.meldehub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

/**
 * Güvenlik kuralları (CASE-201, ADR-0009) — stateless JWT, rol bazlı yetki:
 *
 *   POST /api/auth/login          → herkese açık (token buradan alınır)
 *   /v3/api-docs/**, /swagger-ui/** → herkese açık (API dokümantasyonu)
 *   /actuator/health              → herkese açık (Docker healthcheck bunu çağırır)
 *   POST /api/cases               → CITIZEN veya OPERATOR (ihbar verme)
 *   GET /api/cases/**             → sadece OPERATOR (vaka listesi/detay)
 *   PATCH /api/cases/**           → sadece OPERATOR (durum yönetimi)
 *   diğer her şey                 → authenticated
 *
 * Hata biçimi GlobalExceptionHandler ile aynı: {"error": "..."}
 *   401 — token yok veya bozuk | 403 — token var ama rol yetmez
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, ObjectMapper objectMapper) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Stateless REST API — CSRF token mekanizması cookie oturumuna dayanır; JWT Bearer'da yok
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cases").hasAnyRole("CITIZEN", "OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/cases", "/api/cases/**").hasRole("OPERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/cases/**").hasRole("OPERATOR")
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(forbiddenHandler()))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 401 — kimlik yok/bozuk: {"error": "..."} (GlobalExceptionHandler biçimiyle aynı). */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, ex) -> writeJson(response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Kimlik doğrulaması gerekli — geçerli bir Bearer token gönderin");
    }

    /** 403 — kimlik var ama rol yetmez. */
    private AccessDeniedHandler forbiddenHandler() {
        return (request, response, ex) -> writeJson(response,
                HttpServletResponse.SC_FORBIDDEN,
                "Bu işlem için yetkiniz yok");
    }

    private void writeJson(HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}

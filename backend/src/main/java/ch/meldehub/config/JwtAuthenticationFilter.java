package ch.meldehub.config;

import ch.meldehub.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT filtresi — her istekte Authorization: Bearer <token> başlığını doğrular (CASE-201).
 *
 * Stateless: sunucuda oturum tutulmaz; kimlik tamamen token'dan kurulur.
 * Token yoksa istek anonim devam eder → korunan endpoint'lerde
 * AuthenticationEntryPoint 401 döner. Token bozuksa da aynı yol işler
 * (ayrıntı logda kalır, istemciye sızıntı olmaz).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            try {
                Claims claims = jwtService.parse(header.substring(PREFIX.length()));
                Role role = jwtService.extractRole(claims);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // Bozuk/süresi dolmuş token → anonim bırak; 401'i entry point üretir
                logger.debug("JWT doğrulanamadı: " + ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}

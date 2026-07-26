package ch.meldehub.config;

import ch.meldehub.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT üretimi ve doğrulaması (CASE-201, ADR-0009).
 *
 * HS256 (simetrik imza) — tek servis olduğumuz için anahtar dağıtımı derdi yok.
 * Secret config'ten gelir (app.jwt.secret ← JWT_SECRET env); koda gömülmez.
 * Token içeriği: sub=username, role claim'i, 8 saat geçerlilik.
 * Refresh token bilinçli olarak YOK (demo kapsamı — ADR-0009).
 */
@Service
public class JwtService {

    public static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration validity;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.validity-hours:8}") long validityHours) {
        // HS256 en az 256-bit (32 bayt) anahtar ister — kısa secret burada patlar (fail fast)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validity = Duration.ofHours(validityHours);
    }

    /** İmzalı token üretir: sub=username, role claim'i, 8 saat geçerlilik. */
    public String generateToken(String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /**
     * Token'ı doğrular ve claim'leri döndürür.
     * İmza bozuk, süre dolmuş veya biçim geçersizse JwtException fırlatır.
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Claim'deki role değerini enum'a çevirir; bilinmeyen rol JwtException sayılır. */
    public Role extractRole(Claims claims) {
        String value = claims.get(ROLE_CLAIM, String.class);
        if (value == null) {
            throw new JwtException("Token'da role claim'i yok");
        }
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new JwtException("Bilinmeyen rol: " + value);
        }
    }
}

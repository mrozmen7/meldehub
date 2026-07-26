package ch.meldehub.api;

import ch.meldehub.config.JwtService;
import ch.meldehub.domain.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Kimlik doğrulama ucu (CASE-201).
 *
 * POST /api/auth/login — kullanıcı adı + şifre doğrulanır, başarıda
 * 8 saat geçerli JWT döner. Başarısızlıkta 401; mesaj bilinçli olarak
 * "kullanıcı mı şifre mi yanlış" ayrımı YAPMAZ (kullanıcı adı keşfini zorlaştırır).
 * Hata biçimi GlobalExceptionHandler ile aynı: {"error": "..."}
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AppUserRepository users,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return users.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .map(user -> ResponseEntity.ok((Object) new LoginResponse(
                        jwtService.generateToken(user.getUsername(), user.getRole()),
                        user.getUsername(),
                        user.getRole().name())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Kullanıcı adı veya şifre hatalı")));
    }
}

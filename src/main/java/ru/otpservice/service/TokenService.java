package ru.otpservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ru.otpservice.config.AppConfig;
import ru.otpservice.model.Role;
import ru.otpservice.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Сервис генерации и проверки JWT-токенов
 *
 * Алгоритм подписи — HS256 (симметричный HMAC-SHA-256). Секрет берётся из
 * application.properties поле jwt.secret.
 * Структура claims в нашем токене:
 * sub (subject) = id пользователя
 * username = логин
 * - role = ADMIN или USER
 * - iat = время выпуска
 * - exp = время истечения (now + jwt.ttl.minutes)
 */
public class TokenService {

    // Ключ, на котором подписываются и проверяются токены
    private final SecretKey key;

    // Срок жизни токена в минутах (конфигурируется в application.properties)
    private final long ttlMinutes;

    public TokenService(AppConfig config) {
        // Секрет конвертируется в SecretKey, пригодный для HS256.
        this.key = Keys.hmacShaKeyFor(config.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = config.getJwtTtlMinutes();
    }

    //Выпускает свежий JWT для пользователя, в payload кладутся id, username, role. Срок действия отсчитывается от текущего момента
    public String issue(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMinutes * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    //Парсит и валидирует токен. Если подпись неверна или срок истёк — JJWT кинет исключение, которое в дальнейшем превратиться в 401.
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Геттер userId из claims (subject хранится как строка)
    public Long extractUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    // Геттер роли из claims
    public Role extractRole(Claims claims) {
        return Role.valueOf((String) claims.get("role"));
    }
}

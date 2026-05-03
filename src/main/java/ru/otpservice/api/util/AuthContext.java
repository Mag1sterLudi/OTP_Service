package ru.otpservice.api.util;

import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import ru.otpservice.model.Role;
import ru.otpservice.service.TokenService;

import java.util.Optional;

/**
 * Контекст аутентифицированного пользователя в текущем HTTP-запросе.
 * Создаётся через статический фабричный метод HttpExchange, TokenService,
 * который читает заголовок Authorization: Bearer, парсит токен и достаёт
 * из claims id пользователя и роль.
 * Используется в AdminHandler и UserHandler. Пустой Optional - 401,
 * непустой != ADMIN на эндпоинте админа - 403, иначе доступ разрешён.
 */

public final class AuthContext {

    private final Long userId;
    private final Role role;

    private AuthContext(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public Role getRole() { return role; }

    /**
     * Получает AuthContext из текущего HTTP-обмена.
     * Возвращает пустой Optional, если заголовок Authorization отсутствует или не начинается с Bearer;
     * токен невалидный (недействительная подпись, истёк, не парсится).
     */
    public static Optional<AuthContext> from(HttpExchange exchange, TokenService tokenService) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        // Отрезаем префикс Bearer, остаётся сам JWT
        String token = header.substring("Bearer ".length()).trim();
        try {
            Claims claims = tokenService.parse(token);
            return Optional.of(new AuthContext(
                    tokenService.extractUserId(claims),
                    tokenService.extractRole(claims)
            ));
        } catch (Exception e) {
            // Любая ошибка парсинга/верификации ведёт к тому, что приложение считает юзера неавторизованным
            return Optional.empty();
        }
    }
}

package ru.otpservice.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.api.util.AuthContext;
import ru.otpservice.api.util.JsonUtil;
import ru.otpservice.dao.OtpCodeDao;
import ru.otpservice.model.OtpConfig;
import ru.otpservice.model.Role;
import ru.otpservice.model.User;
import ru.otpservice.service.OtpConfigService;
import ru.otpservice.service.TokenService;
import ru.otpservice.service.UserService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Обработчик эндпоинтов админа:
- GET /api/admin/config (получить текущую конфигурацию OTP);
- PUT|PATCH /api/admin/config (обновить конфигурацию (длина кода и TTL));
- GET /api/admin/users (список пользователей (без админов));
- DELETE /api/admin/users (удалить пользователя и все его OTP-коды)
 Все эндпоинты доступны только админу: без токена - 401, с токеном USER - 403, с токеном ADMIN -
 дальше по логике каждого эндпоинта.
 */
public class AdminHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

    // Префикс пути для DELETE /api/admin/users/{id}
    private static final String USERS_PREFIX = "/api/admin/users/";

    private final UserService userService;
    private final OtpConfigService otpConfigService;
    private final OtpCodeDao otpCodeDao;
    private final TokenService tokenService;

    public AdminHandler(UserService userService,
                        OtpConfigService otpConfigService,
                        OtpCodeDao otpCodeDao,
                        TokenService tokenService) {
        this.userService = userService;
        this.otpConfigService = otpConfigService;
        this.otpCodeDao = otpCodeDao;
        this.tokenService = tokenService;
    }

    //Проверка авторизации и роли, маршрутизация по методу/пути.
    //Стандартная обработка исключений - 400 для IllegalArgumentException, 500 для всего остального.

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        log.info("{} {}", method, path);

        try {
            // Должен быть валидный JWT
            Optional<AuthContext> ctxOpt = AuthContext.from(exchange, tokenService);
            if (ctxOpt.isEmpty()) {
                JsonUtil.writeError(exchange, 401, "Authorization required");
                return;
            }
            AuthContext ctx = ctxOpt.get();
            // И в нём роль ADMIN
            if (ctx.getRole() != Role.ADMIN) {
                log.warn("Forbidden: userId={} tried admin endpoint {}", ctx.getUserId(), path);
                JsonUtil.writeError(exchange, 403, "Forbidden");
                return;
            }

            // Маршрутизация по методу и пути
            if ("GET".equals(method) && path.endsWith("/config")) {
                handleGetConfig(exchange);
            } else if (("PUT".equals(method) || "PATCH".equals(method)) && path.endsWith("/config")) {
                handleUpdateConfig(exchange);
            } else if ("GET".equals(method) && path.endsWith("/users")) {
                handleListUsers(exchange);
            } else if ("DELETE".equals(method) && path.startsWith(USERS_PREFIX)) {
                handleDeleteUser(exchange, path);
            } else {
                JsonUtil.writeError(exchange, 404, "Not Found");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            JsonUtil.writeError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            JsonUtil.writeError(exchange, 500, "Internal Server Error");
        } finally {
            exchange.close();
        }
    }

    // GET /api/admin/config — отдаёт текущую длину кода и TTL
    private void handleGetConfig(HttpExchange exchange) throws IOException {
        OtpConfig cfg = otpConfigService.get();
        JsonUtil.writeJson(exchange, 200, Map.of(
                "codeLength", cfg.getCodeLength(),
                "ttlSeconds", cfg.getTtlSeconds()
        ));
    }

    //PUT/PATCH /api/admin/config — меняет длину кода и TTL.
    // Тело - {"codeLength": 6, "ttlSeconds": 300}.
    @SuppressWarnings("unchecked")
    private void handleUpdateConfig(HttpExchange exchange) throws IOException {
        Map<String, Object> body = JsonUtil.readBody(exchange, Map.class);
        Object lenObj = body.get("codeLength");
        Object ttlObj = body.get("ttlSeconds");
        if (lenObj == null || ttlObj == null) {
            throw new IllegalArgumentException("codeLength and ttlSeconds are required");
        }
        int length = ((Number) lenObj).intValue();
        int ttl = ((Number) ttlObj).intValue();
        otpConfigService.update(length, ttl);
        JsonUtil.writeJson(exchange, 200, Map.of(
                "codeLength", length,
                "ttlSeconds", ttl
        ));
    }

    //api/admin/users — список всех пользователей кроме админов.
    // В ответе нет password_hash — данные наружу не уходят
    private void handleListUsers(HttpExchange exchange) throws IOException {
        List<User> users = userService.listNonAdmins();
        List<Map<String, Object>> response = users.stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("role", u.getRole().name());
                    return m;
                })
                .toList();
        JsonUtil.writeJson(exchange, 200, response);
    }

    //DELETE /api/admin/users/{id} — удаляет пользователя и все его OTP-коды
    // При попытке удаления админа возвращается 404

    private void handleDeleteUser(HttpExchange exchange, String path) throws IOException {
        // Достаём id пользователя из хвоста пути /api/admin/users/{id}
        String tail = path.substring(USERS_PREFIX.length());
        if (tail.isBlank()) {
            JsonUtil.writeError(exchange, 400, "User id is required");
            return;
        }
        long id;
        try {
            id = Long.parseLong(tail);
        } catch (NumberFormatException e) {
            JsonUtil.writeError(exchange, 400, "Invalid user id");
            return;
        }

        // Не удаляем админа — для пользователя возвращаем 404 (сделано, чтобы явно не демонстрировать статус роли)
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty() || userOpt.get().getRole() == Role.ADMIN) {
            JsonUtil.writeError(exchange, 404, "User not found");
            return;
        }

        int otps = otpCodeDao.deleteByUser(id);
        boolean removed = userService.delete(id);
        log.info("Deleted user id={} along with {} OTP codes (removed={})", id, otps, removed);
        JsonUtil.writeJson(exchange, 200, Map.of(
                "deletedUserId", id,
                "deletedOtpCount", otps
        ));
    }
}

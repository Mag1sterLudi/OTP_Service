package ru.otpservice.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.api.util.JsonUtil;
import ru.otpservice.model.Role;
import ru.otpservice.model.User;
import ru.otpservice.service.TokenService;
import ru.otpservice.service.UserService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/** Обработчик публичных эндпоинтов аутентификации:
- POST /api/auth/register (регистрация нового пользователя)
 - POST /api/auth/login (логин с выдачей JWT)
 */

public class AuthHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

    private final UserService userService;
    private final TokenService tokenService;

    public AuthHandler(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    /** Ловим исключения и превращаем их в HTTP-статусы
     - IllegalArgumentException → 400 (некорректный запрос);
     - IllegalStateException → 409 (второй админ, дубликат логина);
     - всё остальное → 500.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        log.info("{} {}", method, path);

        try {
            if ("POST".equals(method) && path.endsWith("/register")) {
                handleRegister(exchange);
            } else if ("POST".equals(method) && path.endsWith("/login")) {
                handleLogin(exchange);
            } else {
                JsonUtil.writeError(exchange, 404, "Not Found");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            JsonUtil.writeError(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Conflict: {}", e.getMessage());
            JsonUtil.writeError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            JsonUtil.writeError(exchange, 500, "Internal Server Error");
        } finally {
            exchange.close();
        }
    }

    // POST /api/auth/register — регистрирует нового пользователя
    // Если роль не передана — по умолчанию USER

    @SuppressWarnings("unchecked")
    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, Object> body = JsonUtil.readBody(exchange, Map.class);
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String roleStr = (String) body.getOrDefault("role", "USER");

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr);
        }

        User user = userService.register(username, password, role);
        JsonUtil.writeJson(exchange, 201, Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole().name()
        ));
    }

    //POST /api/auth/login — проверяет учетные данные и выдаёт JWT
    // Возвращает 401 при невалидных учетных данных (далее по коду - креды)

    @SuppressWarnings("unchecked")
    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, Object> body = JsonUtil.readBody(exchange, Map.class);
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isEmpty()) {
            log.warn("Failed login attempt for username '{}'", username);
            JsonUtil.writeError(exchange, 401, "Invalid credentials");
            return;
        }
        User user = userOpt.get();
        String token = tokenService.issue(user);
        log.info("User '{}' logged in", username);
        JsonUtil.writeJson(exchange, 200, Map.of(
                "token", token,
                "role", user.getRole().name()
        ));
    }
}

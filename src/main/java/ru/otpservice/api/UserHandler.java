package ru.otpservice.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.api.util.AuthContext;
import ru.otpservice.api.util.JsonUtil;
import ru.otpservice.model.OtpCode;
import ru.otpservice.service.OtpService;
import ru.otpservice.service.TokenService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Обработчик пользовательских эндпоинтов
 - POST /api/otp/generate (сгенерировать код и отправить в выбранный канал)
 - POST /api/otp/validate (проверить введённый код)
 Доступно только аутентифицированным пользователям, без валидного JWT — 401.
 */
public class UserHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(UserHandler.class);

    private final OtpService otpService;
    private final TokenService tokenService;

    public UserHandler(OtpService otpService, TokenService tokenService) {
        this.otpService = otpService;
        this.tokenService = tokenService;
    }

    // Маршрутизация запросов - проверяется JWT (если его нет, то выдается 401).

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        log.info("{} {}", method, path);

        try {
            // Любой пользовательский запрос требует валидного JWT
            Optional<AuthContext> ctxOpt = AuthContext.from(exchange, tokenService);
            if (ctxOpt.isEmpty()) {
                JsonUtil.writeError(exchange, 401, "Authorization required");
                return;
            }
            Long userId = ctxOpt.get().getUserId();

            if ("POST".equals(method) && path.endsWith("/generate")) {
                handleGenerate(exchange, userId);
            } else if ("POST".equals(method) && path.endsWith("/validate")) {
                handleValidate(exchange, userId);
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

    // POST /api/otp/generate — генерирует код и отправляет в указанный канал.
    // Код наружу не возвращается, пользователь получит его через выбранный канал.
    @SuppressWarnings("unchecked")
    private void handleGenerate(HttpExchange exchange, Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.readBody(exchange, Map.class);
        String operationId = (String) body.get("operationId");
        // Канал по умолчанию — file (это ничего не требует от внешнего мира).
        String channel = (String) body.getOrDefault("channel", "file");
        String destination = (String) body.get("destination");

        OtpCode otp = otpService.generate(userId, operationId, channel, destination);
        // В ответе только id операции, канал, статус и срок жизни.
        JsonUtil.writeJson(exchange, 200, Map.of(
                "operationId", otp.getOperationId() == null ? "" : otp.getOperationId(),
                "channel", channel,
                "expiresAt", otp.getExpiresAt().toString(),
                "status", otp.getStatus().name()
        ));
    }

    // POST /api/otp/validate — проверяет введённый пользователем код.
    @SuppressWarnings("unchecked")
    private void handleValidate(HttpExchange exchange, Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.readBody(exchange, Map.class);
        String code = (String) body.get("code");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        boolean valid = otpService.validate(userId, code);
        if (valid) {
            JsonUtil.writeJson(exchange, 200, Map.of("valid", true));
        } else {
            JsonUtil.writeJson(exchange, 400, Map.of("valid", false));
        }
    }
}

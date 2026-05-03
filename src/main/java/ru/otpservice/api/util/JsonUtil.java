package ru.otpservice.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/** Класс для работы с JSON при HTTP-обмене.
 * Все хендлеры используют его, чтобы читать тело входящего запроса через HttpExchange, Class,
 * писать JSON-ответ через HttpExchange, int, Object и писать ошибки в едином формате
 **/

 //Маппинг через Jackson
public final class JsonUtil {

    //Общий потокобезопасный ObjectMapper
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    //Десериализуем тело запроса в объект указанного типа
    public static <T> T readBody(HttpExchange exchange, Class<T> type) throws IOException {
        return MAPPER.readValue(exchange.getRequestBody(), type);
    }

    // Сериализуем объект в JSON и отдаём ответ с указанным статусом
    public static void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Ответ с ошибкой
    public static void writeError(HttpExchange exchange, int status, String message) throws IOException {
        writeJson(exchange, status, Map.of("error", message));
    }
}

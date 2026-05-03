package ru.otpservice.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Отправка кода через Telegram Bot API.
 * Используется HttpClient (без сторонних библиотек).
 * Запрос — GET на https://api.telegram.org/bot<TOKEN>/sendMessage с параметрами
 * chat_id и text в query-string
 * chat_id берётся из telegram.properties это id диалога с ботом —
 * один на конкретного пользователя. Параметр destination в sendCode
 * подставляется в текст сообщения как имя получателя.
 */
public class TelegramNotificationService implements NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final String telegramApiUrl;
    private final String chatId;

    public TelegramNotificationService() {
        Properties props = loadConfig();
        String botToken = props.getProperty("telegram.bot.token");
        this.chatId = props.getProperty("telegram.default.chat_id");
        // Полный URL метода sendMessage конкретного бота (токен — часть пути).
        this.telegramApiUrl = props.getProperty("telegram.api.url") + botToken + "/sendMessage";
    }

    // Шлёт сообщение в Telegram, destination подставляется в текст как имя адресата
    @Override
    public void sendCode(String destination, String code) {
        // Формируем текст сообщения с кодом подтверждения
        String message = String.format("%s, your confirmation code is: %s", destination, code);

        // Собираем URL для запроса к Telegram Bot API
        String url = String.format("%s?chat_id=%s&text=%s",
                telegramApiUrl,
                chatId,
                urlEncode(message));

        // Передаём готовый URL в метод отправки запроса
        sendTelegramRequest(url);
    }

    // Делаем GET-запрос к Telegram API и логируем результат
    private void sendTelegramRequest(String url) {
        // Создаём HTTP-клиент из стандартной библиотеки Java
        HttpClient httpClient = HttpClient.newHttpClient();

        // Создаём GET-запрос к Telegram API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            // Отправляем запрос и получаем ответ в виде строки
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Проверяем HTTP-статус ответа
            int statusCode = response.statusCode();
            if (statusCode != 200) {
                logger.error("Telegram API error. Status code: {}", statusCode);
            } else {
                logger.info("Telegram message sent successfully");
            }
        } catch (InterruptedException e) {
            // Если поток был прерван, логируем ошибку и восстанавливаем флаг прерывания
            logger.error("Error sending Telegram message: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Если произошла ошибка ввода-вывода, логируем её
            logger.error("Error sending Telegram message: {}", e.getMessage(), e);
        }
    }

    // Url-encode для текста сообщения (пробелы, кириллица и т.д.)
    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // Загружаес telegram.properties
    private Properties loadConfig() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("telegram.properties")) {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load telegram.properties", e);
        }
    }
}

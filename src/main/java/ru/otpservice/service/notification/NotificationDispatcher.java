package ru.otpservice.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Маршрутизатор способов доставки, который получает строковое имя канала
 * (приходит из тела HTTP-запроса) и делегирует отправку нужной реализации NotificationChannel
 * Если канал неизвестен — fallback в файл
 */
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationChannel email;
    private final NotificationChannel sms;
    private final NotificationChannel telegram;
    private final NotificationChannel file;

    public NotificationDispatcher(NotificationChannel email,
                                  NotificationChannel sms,
                                  NotificationChannel telegram,
                                  NotificationChannel file) {
        this.email = email;
        this.sms = sms;
        this.telegram = telegram;
        this.file = file;
    }
    // Отправляет код в указанный канал
    public void send(String channel, String destination, String code) {
        if (channel == null) {
            throw new IllegalArgumentException("channel is required");
        }
        // Имя канала из API нечувствительно к регистру.
        switch (channel.toLowerCase(Locale.ROOT)) {
            case "email":
                email.sendCode(destination, code);
                break;
            case "sms":
                sms.sendCode(destination, code);
                break;
            case "telegram":
                telegram.sendCode(destination, code);
                break;
            case "file":
                file.sendCode(destination, code);
                break;
            default:
                // Если клиент прислал что-то нестандартное — пишем в файл, чтобы код не потерялся.
                log.warn("Unknown channel '{}', falling back to file", channel);
                file.sendCode(destination, code);
        }
    }
}

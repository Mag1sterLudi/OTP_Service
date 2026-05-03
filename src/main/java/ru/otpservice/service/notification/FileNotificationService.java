package ru.otpservice.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Сохранение кода в файл
 * Сгенерированный код дописывается отдельной строкой в файл otp-codes.log
 * в корне проекта. Полезно при отладке и проверке без email/SMS/Telegram
 */

public class FileNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(FileNotificationService.class);

    // Файл создаётся в текущей рабочей директории (обычно — корень проекта)
    private static final Path FILE = Path.of("otp-codes.log");

    // Дописывает строку с кодом в файл, который создаётся при первом вызове. Открывается в режиме APPEND, чтобы не затирать предыдущие записи.
    @Override
    public void sendCode(String destination, String code) {
        String line = String.format("%s | destination=%s | code=%s%n",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                destination == null ? "-" : destination,
                code);
        try {
            Files.writeString(
                    FILE,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            log.info("OTP code saved to file {}", FILE.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write OTP to file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to write OTP to file", e);
        }
    }
}

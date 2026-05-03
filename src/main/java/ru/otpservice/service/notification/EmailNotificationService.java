package ru.otpservice.service.notification;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Отправка кода по электронной почте через SMTP. Реализация на Jakarta Mail (Angus Mail).
 * Все параметры подключения (хост, порт, учетные данные отправителя) лежат в email.properties
 * В моей реализации, для удобства проверяющего, настроен эмулятор SMTP локальный Mailpit (localhost:1025}, без авторизации)
 * Для отправки на реальный Gmail/Yandex достаточно поменять properties без изменений в коде
 * destination в методе sendCode(String, String) — адрес получателя, который приходит из тела HTTP-запроса
 */
public class EmailNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final String username;
    private final String password;
    private final String fromEmail;
    private final Session session;

    public EmailNotificationService() {
        Properties config = loadConfig();
        this.username = config.getProperty("email.username");
        this.password = config.getProperty("email.password");
        this.fromEmail = config.getProperty("email.from");
        // Создаём SMTP-сессию с тем же набором properties (mail.smtp.host, port и пр. передаются как есть).
        this.session = Session.getInstance(config, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /**
     * Отправляет письмо с кодом получателю toEmail
     * При сбое ловим MessagingException, который в дальнейшем будет HTTP 500 для клиента
     */
    @Override
    public void sendCode(String toEmail, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);
            Transport.send(message);
            log.info("Email with OTP sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // Загружает email.properties, кидает исключение, если файла нет
    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(EmailNotificationService.class.getClassLoader()
                    .getResourceAsStream("email.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load email configuration", e);
        }
    }
}

package ru.otpservice;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.api.AdminHandler;
import ru.otpservice.api.AuthHandler;
import ru.otpservice.api.UserHandler;
import ru.otpservice.config.AppConfig;
import ru.otpservice.dao.DatabaseManager;
import ru.otpservice.dao.OtpCodeDao;
import ru.otpservice.dao.OtpConfigDao;
import ru.otpservice.dao.UserDao;
import ru.otpservice.scheduler.OtpExpirationScheduler;
import ru.otpservice.service.OtpConfigService;
import ru.otpservice.service.OtpService;
import ru.otpservice.service.TokenService;
import ru.otpservice.service.UserService;
import ru.otpservice.service.notification.EmailNotificationService;
import ru.otpservice.service.notification.FileNotificationService;
import ru.otpservice.service.notification.NotificationDispatcher;
import ru.otpservice.service.notification.SmsNotificationService;
import ru.otpservice.service.notification.TelegramNotificationService;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

//Точка входа в приложение.

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();

        // БД и DAO
        // DatabaseManager отвечает за подключения и первичную инициализацию схемы.
        DatabaseManager db = new DatabaseManager(config);
        db.initSchema();

        UserDao userDao = new UserDao(db);
        OtpConfigDao otpConfigDao = new OtpConfigDao(db);
        OtpCodeDao otpCodeDao = new OtpCodeDao(db);
        // Если конфига ещё нет — создаётся дефолтная запись (длина 6, TTL 300 секунд).
        otpConfigDao.ensureDefaultConfig();

        // Сервисы
        TokenService tokenService = new TokenService(config);
        UserService userService = new UserService(userDao);
        OtpConfigService otpConfigService = new OtpConfigService(otpConfigDao);

        // Каналы доставки кодов
        // Каждый канал сам читает свой properties при создании.
        EmailNotificationService email = new EmailNotificationService();
        SmsNotificationService sms = new SmsNotificationService();
        TelegramNotificationService telegram = new TelegramNotificationService();
        FileNotificationService file = new FileNotificationService();

        NotificationDispatcher dispatcher = new NotificationDispatcher(email, sms, telegram, file);
        OtpService otpService = new OtpService(otpCodeDao, otpConfigService, dispatcher);

        // Фоновая задача истечения OTP
        // Раз в N секунд (по умолчанию 30) переводит просроченные ACTIVE-коды в EXPIRED.
        OtpExpirationScheduler scheduler = new OtpExpirationScheduler(otpCodeDao, config);
        scheduler.start();

        // HTTP сервер на com.sun.net.httpserver
        // Используется встроенный JDK-сервер без сторонних библиотек
        HttpServer server = HttpServer.create(new InetSocketAddress(config.getServerPort()), 0);
        server.createContext("/api/auth", new AuthHandler(userService, tokenService));
        server.createContext("/api/admin", new AdminHandler(userService, otpConfigService, otpCodeDao, tokenService));
        server.createContext("/api/otp", new UserHandler(otpService, tokenService));
        // Пул из 8 потоков — для учебной нагрузки достаточно.
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        log.info("OTP service started on port {}", config.getServerPort());

        // Комбинация Ctrl+C позволит корректно остановить приложение)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            scheduler.stop();
            server.stop(1);
        }));
    }
}

package ru.otpservice.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Загрузчик настроек приложения из application.properties
 * Конструктор приватный: создание объекта возможно только через статический
 * фабричный метод load(), который читает файл из ресурсов проекта и парсит нужные ключи.
 * Конфигурация неизменяемая после загрузки (поля final). Если нужно
 * поменять параметры — правится файл и приложение перезапускается.
 */
public class AppConfig {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int serverPort;
    private final String jwtSecret;
    private final long jwtTtlMinutes;
    private final long expireSchedulerPeriodSeconds;

    // Парсит  Properties в поля, при это для необязательных параметров заданы дефолтные значения
    private AppConfig(Properties props) {
        this.dbUrl = props.getProperty("db.url");
        this.dbUser = props.getProperty("db.user");
        this.dbPassword = props.getProperty("db.password");
        this.serverPort = Integer.parseInt(props.getProperty("server.port", "8080"));
        this.jwtSecret = props.getProperty("jwt.secret");
        this.jwtTtlMinutes = Long.parseLong(props.getProperty("jwt.ttl.minutes", "60"));
        this.expireSchedulerPeriodSeconds = Long.parseLong(
                props.getProperty("scheduler.expire.period.seconds", "30"));
    }

    // application.properties из classpath и собирает AppConfig
    // пробрасывает RuntimeException, если файл не найден или не парсится
    // С учетом того, что приложение без конфига работать не может - оно упадет
    public static AppConfig load() {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new RuntimeException("application.properties not found in classpath");
            }
            Properties p = new Properties();
            p.load(in);
            return new AppConfig(p);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public int getServerPort() { return serverPort; }
    public String getJwtSecret() { return jwtSecret; }
    public long getJwtTtlMinutes() { return jwtTtlMinutes; }
    public long getExpireSchedulerPeriodSeconds() { return expireSchedulerPeriodSeconds; }
}

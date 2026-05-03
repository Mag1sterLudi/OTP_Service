package ru.otpservice.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.config.AppConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Управление подключениями к БД и первичной инициализацией схемы.
 * Реализован самым простым способом - каждый DAO получает Connection из DriverManager и закрывает его сразу после операции
 * (через try-with-resources).
 * При создании DatabaseManager один раз загружается JDBC-драйвер PostgreSQL.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(AppConfig config) {
        this.url = config.getDbUrl();
        this.user = config.getDbUser();
        this.password = config.getDbPassword();
        try {
            // Явная регистрация драйвера на случай старых версий JDBC
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL driver not found", e);
        }
    }

    // Открывает новое соединение с БД
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Инициализирует схему БД из файла schema.sql, при этом файл читается целиком, разбивается по ";"
     * и каждый CREATE TABLE выполняется по отдельности.
     * Все операторы используют IF NOT EXISTS}, поэтому повторный вызов безопасен.
     */
    public void initSchema() {
        log.info("Initializing database schema...");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new RuntimeException("schema.sql not found in classpath");
            }
            // Читаем весь SQL-файл одной строкой
            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }
            // И выполняем каждый оператор по очереди
            try (Connection con = getConnection(); Statement st = con.createStatement()) {
                for (String stmt : sql.split(";")) {
                    String trimmed = stmt.trim();
                    if (!trimmed.isEmpty()) {
                        st.execute(trimmed);
                    }
                }
            }
            log.info("Schema initialized");
        } catch (Exception e) {
            throw new RuntimeException("Failed to init schema", e);
        }
    }
}

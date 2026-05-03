package ru.otpservice.dao;

import ru.otpservice.model.OtpConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

//DAO для таблицы otp_config
public class OtpConfigDao {

    // Дефолтная длина кода при первичной инициализации (6 цифр)
    private static final int DEFAULT_LENGTH = 6;

    // Дефолтный TTL кода при первичной инициализации (5 минут)
    private static final int DEFAULT_TTL_SECONDS = 300;

    private final DatabaseManager db;

    public OtpConfigDao(DatabaseManager db) {
        this.db = db;
    }

    // Загружает единственную запись конфига. Возвращает пустой Optional, если таблица пуста
    public Optional<OtpConfig> find() {
        String sql = "SELECT id, code_length, ttl_seconds FROM otp_config LIMIT 1";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                OtpConfig c = new OtpConfig();
                c.setId(rs.getLong("id"));
                c.setCodeLength(rs.getInt("code_length"));
                c.setTtlSeconds(rs.getInt("ttl_seconds"));
                return Optional.of(c);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load otp config", e);
        }
    }

    // Обновляет конфигурацию OTP
    public void update(int codeLength, int ttlSeconds) {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codeLength);
            ps.setInt(2, ttlSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update otp config", e);
        }
    }

    // Создаёт дефолтную запись, если таблица пустая
    public void ensureDefaultConfig() {
        if (find().isPresent()) {
            return;
        }
        String sql = "INSERT INTO otp_config (code_length, ttl_seconds) VALUES (?, ?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, DEFAULT_LENGTH);
            ps.setInt(2, DEFAULT_TTL_SECONDS);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert default otp config", e);
        }
    }
}

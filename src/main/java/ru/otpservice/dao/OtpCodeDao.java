package ru.otpservice.dao;

import ru.otpservice.model.OtpCode;
import ru.otpservice.model.OtpStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * DAO для таблицы otp_codes
 * Хранит сами OTP-коды и их жизненный цикл
 * Связан с пользователем через user_id с ON DELETE CASCADE,
 * так что удаление юзера автоматически удалит и его коды.
 * + дублирование через deleteByUser(Long) для надёжности.
 */
public class OtpCodeDao {

    private final DatabaseManager db;

    public OtpCodeDao(DatabaseManager db) {
        this.db = db;
    }

    //Сохраняет новый код в БД и проставляет его id, operation_id может быть null и тогда используется setNull

    public OtpCode save(OtpCode otp) {
        String sql = "INSERT INTO otp_codes (user_id, operation_id, code, status, created_at, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, otp.getUserId());
            if (otp.getOperationId() == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, otp.getOperationId());
            }
            ps.setString(3, otp.getCode());
            ps.setString(4, otp.getStatus().name());
            ps.setTimestamp(5, Timestamp.valueOf(otp.getCreatedAt()));
            ps.setTimestamp(6, Timestamp.valueOf(otp.getExpiresAt()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    otp.setId(rs.getLong(1));
                }
            }
            return otp;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save otp", e);
        }
    }

    // Ищем ACTIVE-код пользователя по значению
    public Optional<OtpCode> findActiveByUserAndCode(Long userId, String code) {
        String sql = "SELECT id, user_id, operation_id, code, status, created_at, expires_at " +
                "FROM otp_codes WHERE user_id = ? AND code = ? AND status = 'ACTIVE'";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find otp", e);
        }
    }

    //Изменяет статус конкретного кода (используется для перевода в USED или EXPIRED)
    public void updateStatus(Long id, OtpStatus status) {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update otp status", e);
        }
    }

    /**
     * Помечает все ACTIVE-коды с истёкшим expires_at как EXPIRED
     * Вызывается из ru.otpservice.scheduler.OtpExpirationScheduler раз в N секунд
     * Возвращает количество обновлённых строк (для логов)
     */
    public int markExpired(LocalDateTime now) {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' " +
                "WHERE status = 'ACTIVE' AND expires_at < ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark expired otps", e);
        }
    }

    //Удаление всех OTP-кодов пользователя. Используется при удалении самого пользователя из админского API.
    //Возвращает количество удалённых строк.
    public int deleteByUser(Long userId) {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete otps by user", e);
        }
    }

    // Маппинг строки ResultSet в объект OtpCode
    private OtpCode map(ResultSet rs) throws SQLException {
        OtpCode c = new OtpCode();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("user_id"));
        c.setOperationId(rs.getString("operation_id"));
        c.setCode(rs.getString("code"));
        c.setStatus(OtpStatus.valueOf(rs.getString("status")));
        c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        c.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        return c;
    }
}

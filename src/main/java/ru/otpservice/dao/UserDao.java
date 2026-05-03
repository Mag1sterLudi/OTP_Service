package ru.otpservice.dao;

import ru.otpservice.model.Role;
import ru.otpservice.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для таблицы users, все методы используют JDBC напрямую через
 * DatabaseManager. Каждое подключение открывается и закрывается
 * через try-with-resources.
 */
public class UserDao {

    private final DatabaseManager db;

    public UserDao(DatabaseManager db) {
        this.db = db;
    }

    // Вставляет нового пользователя и проставляет его id
    public User save(User user) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?) RETURNING id";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    // Поиск пользователя по логину
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by username", e);
        }
    }

    // Поиск пользователя по id
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id", e);
        }
    }

    //Используется при регистрации, чтобы запретить создание второго админа.
    public boolean adminExists() {
        String sql = "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check admin existence", e);
        }
    }

    // Список всех пользователей кроме админов, Используется в админском API - GET /api/admin/users
    public List<User> findAllNonAdmins() {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE role <> 'ADMIN' ORDER BY id";
        List<User> result = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch users", e);
        }
    }

    // Удаляет пользователя по id (исключает удаление администратора)
    public boolean delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ? AND role <> 'ADMIN'";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    // Маппинг строки ResultSet в объект User
    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        return u;
    }
}

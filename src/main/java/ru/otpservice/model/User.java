package ru.otpservice.model;

/** Сущность пользователя. Соответствует строке таблицы - users
 * Хранит три поля - логин, хеш пароля и роль
 * Сам пароль в открытом виде в системе не сохраняется
 */
public class User {

    // Первичный ключ, генерируется БД (BIGSERIAL). У ещё не сохранённой сущности — null
    private Long id;

    // Уникальный логин пользователя
    private String username;

    // BCrypt-хеш пароля
    private String passwordHash;

    // Роль пользователя
    private Role role;

    public User() {
    }

    public User(Long id, String username, String passwordHash, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}

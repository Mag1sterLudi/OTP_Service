package ru.otpservice.model;

import java.time.LocalDateTime;

/**
 * Доменная сущность одноразового кода. Соответствует строке таблицы - otp_codes
 * Создаётся со статусом ACTIVE при запросе пользователем
 * Переходит в USED при успешной валидации
 * Переходит в EXPIRED либо при попытке валидации после expiresAt,
 * либо фоновой задачей ru.otpservice.scheduler.OtpExpirationScheduler
 */
public class OtpCode {

    // Первичный ключ, генерируется БД
    private Long id;

    // Идентификатор пользователя, которому принадлежит код
    private Long userId;

    // Идентификатор операции, к которой привязан код. Может быть null
    private String operationId;

    // Сам код
    private String code;

    // Текущий статус кода
    private OtpStatus status;

    // Когда код был создан
    private LocalDateTime createdAt;

    // Когда код перестаёт быть валидным
    private LocalDateTime expiresAt;

    public OtpCode() {
    }

    public OtpCode(Long id, Long userId, String operationId, String code,
                   OtpStatus status, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public OtpStatus getStatus() { return status; }
    public void setStatus(OtpStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}

package ru.otpservice.model;

/** Основная конфигурация OTP. Соответствует строке таблицы - otp_config
 * Менять конфигурацию имеет право только администратор через PUT /api/admin/config
 */
public class OtpConfig {

    //Первичный ключ
    private Long id;

    // Длина генерируемого кода (количество цифр)
    private int codeLength;

    // Время жизни кода в секундах с момента создания
    private int ttlSeconds;

    public OtpConfig() {
    }

    public OtpConfig(Long id, int codeLength, int ttlSeconds) {
        this.id = id;
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }

    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
}

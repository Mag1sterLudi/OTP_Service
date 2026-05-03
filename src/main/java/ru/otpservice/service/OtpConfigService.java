package ru.otpservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.dao.OtpConfigDao;
import ru.otpservice.model.OtpConfig;

/** Сервис управления конфигурацией OTP-кодов
 * Конфиг — единственная запись в таблице otp_config, доступна для изменения только админу
 * через эндпоинт PUT /api/admin/config}. Используется в OtpService при генерации кода
 */
public class OtpConfigService {

    private static final Logger log = LoggerFactory.getLogger(OtpConfigService.class);

    private final OtpConfigDao dao;

    public OtpConfigService(OtpConfigDao dao) {
        this.dao = dao;
    }

    //Возвращает актуальную конфигурацию
    public OtpConfig get() {
        return dao.find().orElseThrow(() -> new IllegalStateException("OTP config is not initialized"));
    }

    //Обновляет конфигурацию (длина кода и TTL в секундах)

    public void update(int codeLength, int ttlSeconds) {
        if (codeLength <= 0 || ttlSeconds <= 0) {
            throw new IllegalArgumentException("codeLength and ttlSeconds must be positive");
        }
        dao.update(codeLength, ttlSeconds);
        log.info("OTP config updated: length={}, ttlSeconds={}", codeLength, ttlSeconds);
    }
}

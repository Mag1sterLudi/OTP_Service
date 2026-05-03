package ru.otpservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.dao.OtpCodeDao;
import ru.otpservice.model.OtpCode;
import ru.otpservice.model.OtpConfig;
import ru.otpservice.model.OtpStatus;
import ru.otpservice.service.notification.NotificationDispatcher;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/** Основная логика OTP — генерация и проверка одноразовых кодов
 * Связывает три компонента:
 - OtpCodeDao (для сохранения и поиска кодов в БД)
 - OtpConfigService (чтобы знать длину кода и TTL)
 - NotificationDispatcher (чтобы отправить код пользователю в нужный канал)
 */
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    // Генератор для случайных цифр кода
    private static final SecureRandom RND = new SecureRandom();

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigService configService;
    private final NotificationDispatcher dispatcher;

    public OtpService(OtpCodeDao otpCodeDao, OtpConfigService configService, NotificationDispatcher dispatcher) {
        this.otpCodeDao = otpCodeDao;
        this.configService = configService;
        this.dispatcher = dispatcher;
    }

    // Генерирует новый код, сохраняет его в БД со статусом ACTIVE и отправляет в указанный канал
    public OtpCode generate(Long userId, String operationId, String channel, String destination) {
        OtpConfig config = configService.get();
        String code = generateCode(config.getCodeLength());

        LocalDateTime now = LocalDateTime.now();
        OtpCode otp = new OtpCode(
                null,
                userId,
                operationId,
                code,
                OtpStatus.ACTIVE,
                now,
                now.plusSeconds(config.getTtlSeconds())
        );
        otpCodeDao.save(otp);
        log.info("OTP generated for userId={}, operationId={}, channel={}", userId, operationId, channel);

        // Доставляем код пользователю. Если в канале случится ошибка (Email/SMPP/TG недоступен),
        // диспетчер кинет исключение, которое поднимется до хендлера и вернётся 500.
        dispatcher.send(channel, destination, code);
        return otp;
    }

    /**
     Проверяем код
     - ищем ACTIVE-код пользователя с таким значением
     - если не нашли (нет такого, USED, EXPIRED) — возвращаем false
     - если нашли, но expiresAt в прошлом — переводим в EXPIRED и возвращаем false
     - иначе переводим в USED и возвращаем true.
     */
    public boolean validate(Long userId, String code) {
        Optional<OtpCode> opt = otpCodeDao.findActiveByUserAndCode(userId, code);
        if (opt.isEmpty()) {
            log.info("OTP validation failed: not found for userId={}", userId);
            return false;
        }
        OtpCode otp = opt.get();
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Код активный, но уже просроченный — помечаем EXPIRED, чтобы шедулеру не пришлось это делать.
            otpCodeDao.updateStatus(otp.getId(), OtpStatus.EXPIRED);
            log.info("OTP validation failed: expired (id={})", otp.getId());
            return false;
        }
        otpCodeDao.updateStatus(otp.getId(), OtpStatus.USED);
        log.info("OTP validated successfully (id={}, userId={})", otp.getId(), userId);
        return true;
    }

    // Генерирует строку из случайных цифр
    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RND.nextInt(10));
        }
        return sb.toString();
    }
}

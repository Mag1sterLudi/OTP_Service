package ru.otpservice.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.config.AppConfig;
import ru.otpservice.dao.OtpCodeDao;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//Фоновая задача, которая сканирует таблицу otp_codes и переводит все ACTIVE-коды с истёкшим
//сроком действия в статус EXPIRED

public class OtpExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final OtpCodeDao otpCodeDao;
    private final long periodSeconds;
    private final ScheduledExecutorService executor;

    public OtpExpirationScheduler(OtpCodeDao otpCodeDao, AppConfig config) {
        this.otpCodeDao = otpCodeDao;
        this.periodSeconds = config.getExpireSchedulerPeriodSeconds();
        // Один поток — для нашей нагрузки больше не нужно. Daemon — чтобы не блокировать shutdown.
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "otp-expirer");
            t.setDaemon(true);
            return t;
        });
    }

    // Запускает периодическое сканирование, первый прогон — через periodSeconds
    public void start() {
        log.info("Starting OTP expiration scheduler with period {}s", periodSeconds);
        executor.scheduleAtFixedRate(this::run, periodSeconds, periodSeconds, TimeUnit.SECONDS);
    }

    // Один тик - помечает просроченные коды и логирует их
    private void run() {
        try {
            int updated = otpCodeDao.markExpired(LocalDateTime.now());
            if (updated > 0) {
                log.info("Marked {} OTP code(s) as EXPIRED", updated);
            }
        } catch (Exception e) {
            // Если БД временно недоступна — логируем и ждём следующего тика, шедулер не валим.
            log.error("Failed to mark expired OTPs: {}", e.getMessage(), e);
        }
    }

    // Корректное завершение — вызывается из shutdown в Main
    public void stop() {
        executor.shutdownNow();
    }
}

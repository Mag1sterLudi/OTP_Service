package ru.otpservice.service.notification;

import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Отправка кода через SMPP-эмулятор (например, SMPPsim).
 * Реализация на библиотеке jSMPP.
 * Механизм отправки SMS:
 * - открываем TCP-соединение с эмулятором и делаем bind (логин по system_id / password)
 * - делаем submit_sm с текстом кода
 * - закрываем сессию.
 * Используется BindType#BIND_TRX (transceiver), потому что встроенный jSMPP-симулятор
 * принимает только этот тип. При реальной отправке можно использовать и BIND_TX.
 */
public class SmsNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsNotificationService() {
        Properties props = loadConfig();
        this.host = props.getProperty("smpp.host");
        this.port = Integer.parseInt(props.getProperty("smpp.port"));
        this.systemId = props.getProperty("smpp.system_id");
        this.password = props.getProperty("smpp.password");
        this.systemType = props.getProperty("smpp.system_type");
        this.sourceAddress = props.getProperty("smpp.source_addr");
    }

    // Открывает SMPP-сессию, отправляет SMS с кодом и закрывает сессию, любая ошибка выдаст 500 на API
    @Override
    public void sendCode(String destination, String code) {
        SMPPSession session = new SMPPSession();
        try {
            // Параметры для bind-запроса. Transceiver = двусторонний канал (симулятору так удобнее).
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TRX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );

            session.connectAndBind(host, port, bindParameter);

            // submit_sm — это «отправка SMS» в SMPP.
            // Параметры идут в фиксированном порядке
            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    ("Your code: " + code).getBytes(StandardCharsets.UTF_8)
            );

            log.info("SMS with OTP sent to {}", destination);
        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        } finally {
            // Закрываем соединение
            session.unbindAndClose();
        }
    }

    // Загружает sms.properties
    private Properties loadConfig() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("sms.properties")) {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sms.properties", e);
        }
    }
}

package ru.otpservice.service.notification;

/**
 * Интерфейс для всех способов доставки OTP, каждая реализация знает, как отправить код в свой конкретный канал.
 * Выбор реализации происходит в NotificationDispatcher по строковому имени канала.
 */
public interface NotificationChannel {
//отправить код выбранным способом
    void sendCode(String destination, String code);
}

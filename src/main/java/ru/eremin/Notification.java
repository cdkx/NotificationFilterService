package ru.eremin;

/**
 * Представляет собой неизменяемую модель уведомления в системе.
 * <p>
 * Содержит всю необходимую информацию для маршрутизации, фильтрации
 * согласно предпочтениям пользователя и последующей доставки сообщения.
 *
 * @param id               уникальный идентификатор уведомления;
 * @param notificationType тип (канал) доставки уведомления (например, EMAIL, SMS, PUSH)
 * @param receiverId       идентификатор получателя
 * @param senderId         идентификатор отправителя
 * @param message          текст сообщения
 */
public record Notification(
        int id,
        NotificationType notificationType,
        int receiverId,
        int senderId,
        String message
) {
}

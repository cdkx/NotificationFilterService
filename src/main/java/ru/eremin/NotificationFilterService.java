package ru.eremin;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class NotificationFilterService {
    private final PreferenceSettingsProvider preferenceSettingsProvider;
    private final HistoryNotificationsProvider historyNotificationsProvider;


    public NotificationFilterService(PreferenceSettingsProvider preferenceSettingsProvider,
                                     HistoryNotificationsProvider historyNotificationsProvider) {
        this.preferenceSettingsProvider = Objects.requireNonNull(preferenceSettingsProvider);
        this.historyNotificationsProvider = Objects.requireNonNull(historyNotificationsProvider);
    }

    public List<Notification> getFilteredNotifications(List<Notification> notifications, int senderId) {
        // 1. Проверка входных данных
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }

        // 2. Сбор уникальных receiverId для запросов к провайдерам
        Set<Integer> receiverIds = notifications.stream()
                .map(Notification::receiverId)
                .collect(Collectors.toSet());

        // 3. Вызов провайдеров
        // Вычисляем cutoffTime = Instant.now().minus(24 часа)
        Instant cutoffTime = Instant.now().minus(Duration.ofHours(24));
        Map<Integer, PreferenceSettings> preferenceSettings = preferenceSettingsProvider.getPreferencesByIds(receiverIds);
        Map<Integer, Set<Integer>> sentNotifications = historyNotificationsProvider.getSentNotificationIds(receiverIds, cutoffTime);

        // Если все фильтры пройдены → добавляем в result
        return notifications.stream()
                .filter(notification -> shouldSend(notification, senderId, preferenceSettings, sentNotifications))
                .toList();
    }

    private boolean shouldSend(Notification notification,
                               int senderId,
                               Map<Integer, PreferenceSettings> preferenceSettings,
                               Map<Integer, Set<Integer>> sentNotifications // Уведомления уже отфильтрованы. Там только те что за последние за 24 часа
    ) {
        return isSenderValid(notification, senderId)
                && isAllowedByPreferences(notification, senderId, preferenceSettings)
                && !isDuplicate(notification, sentNotifications);
    }

    /**
     * Проверяет, соответствует ли идентификатор отправителя в уведомлении ожидаемому.
     *
     * @param notification Проверяем это уведомление
     * @param senderId     идентификатор отправителя для проверки
     * @return {@code true} если notification принадлежит отправителю
     */
    private boolean isSenderValid(Notification notification, int senderId) {
        return senderId == notification.senderId();
    }

    /**
     *
     * @param notification       Проверяем это уведомление
     * @param senderId           идентификатор отправителя для проверки по blockedSenders
     * @param preferenceSettings карта настроек пользователей, где ключом является {@code receiverId}
     * @return {@code true} Если notification подходит под предпочтения
     */
    private boolean isAllowedByPreferences(Notification notification,
                                           int senderId,
                                           Map<Integer, PreferenceSettings> preferenceSettings) {
        // Получаем PreferenceSettings из Map по receiverId
        PreferenceSettings settings = preferenceSettings.get(notification.receiverId());
        if (settings == null) {
            return true;
        }

        //Проверяем allowedChannels (если не пустой, содержит ли тип уведомления?)
        if (!settings.allowedChannels().isEmpty()
                && !settings.allowedChannels().contains(notification.notificationType())) {
            return false;
        }

        //Проверяем blockedSenders (содержит ли senderId?)
        return !settings.blockedSenders().contains(senderId);
    }

    /**
     * Фильтр 3: проверка дублирования
     *
     * @param notification      Проверяем id этого уведомления на содержание в сете уже отправленных id
     * @param sentNotifications Set<Integer> уже отправленных id уведомлений за 24 часа
     * @return Если Set содержит notificationId возвращаем false
     */
    private boolean isDuplicate(Notification notification, Map<Integer, Set<Integer>> sentNotifications) {
        Set<Integer> sentNotificationIds = sentNotifications.getOrDefault(notification.receiverId(), Set.of());
        return sentNotificationIds.contains(notification.id());
    }
}

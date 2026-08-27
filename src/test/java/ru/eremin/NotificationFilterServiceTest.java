package ru.eremin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationFilterService")
class NotificationFilterServiceTest {

    @Mock
    private PreferenceSettingsProvider preferenceSettingsProvider;

    @Mock
    private HistoryNotificationsProvider historyNotificationsProvider;

    private NotificationFilterService service;

    private static final int SENDER_ID = 100;
    private static final int RECEIVER_ID = 200;

    @BeforeEach
    void setUp() {
        service = new NotificationFilterService(preferenceSettingsProvider, historyNotificationsProvider);
    }

    private Notification createNotification(int id, NotificationType type, int receiverId, int senderId) {
        return new Notification(id, type, receiverId, senderId, "Test message " + id);
    }

    private PreferenceSettings createPreferences(boolean notificationsEnabled,
                                                 Set<NotificationType> channels,
                                                 Set<Integer> blockedSenders) {
        return new PreferenceSettings(notificationsEnabled, channels, blockedSenders);
    }

    private void setupAllAllowed() {
        when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                .thenReturn(Map.of(RECEIVER_ID, createPreferences(true, Set.of(NotificationType.EMAIL, NotificationType.SMS, NotificationType.PUSH), Set.of())));
        when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class)))
                .thenReturn(Map.of());
    }

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveScenarios {

        @Test
        @DisplayName("TC1: Уведомление проходит все проверки успешно")
        void shouldPassWhenAllChecksOk() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            setupAllAllowed();

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertEquals(1, result.size());
            assertTrue(result.contains(notification));
        }

        @Test
        @DisplayName("TC2: У пользователя нет настроек (null в Map)")
        void shouldPassWhenNoPreferences() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            when(preferenceSettingsProvider.getPreferencesByIds(Set.of(RECEIVER_ID))).thenReturn(Map.of());
            when(historyNotificationsProvider.getSentNotificationIds(eq(Set.of(RECEIVER_ID)), any(Instant.class))).thenReturn(Map.of());

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("TC3: notificationsEnabled=true + пустой allowedChannels = всё разрешено")
        void shouldPassWhenNotificationsEnabledAndEmptyChannels() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                    .thenReturn(Map.of(RECEIVER_ID, createPreferences(true, Set.of(), Set.of())));
            when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class))).thenReturn(Map.of());

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Негативные сценарии (фильтры)")
    class NegativeScenarios {

        @Test
        @DisplayName("TC4: Фильтрация по несоответствующему отправителю")
        void shouldFilterBySenderId() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, 999);
            setupAllAllowed();

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC5: Фильтрация по запрещенному каналу")
        void shouldFilterByChannel() {
            Notification notification = createNotification(1, NotificationType.SMS, RECEIVER_ID, SENDER_ID);
            when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                    .thenReturn(Map.of(RECEIVER_ID, createPreferences(true, Set.of(NotificationType.EMAIL), Set.of())));
            when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class))).thenReturn(Map.of());

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC6: Фильтрация по заблокированному отправителю")
        void shouldFilterByBlockedSender() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                    .thenReturn(Map.of(RECEIVER_ID, createPreferences(true, Set.of(NotificationType.EMAIL), Set.of(SENDER_ID))));
            when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class))).thenReturn(Map.of());

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC7: Фильтрация дубликата")
        void shouldFilterDuplicate() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                    .thenReturn(Map.of(RECEIVER_ID, createPreferences(true, Set.of(NotificationType.EMAIL), Set.of())));
            when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class)))
                    .thenReturn(Map.of(RECEIVER_ID, Set.of(1)));

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC12: notificationsEnabled=false блокирует ВСЕ уведомления")
        void shouldFilterAllWhenNotificationsDisabled() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                    .thenReturn(Map.of(RECEIVER_ID, createPreferences(false, Set.of(NotificationType.EMAIL, NotificationType.SMS), Set.of())));
            when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class))).thenReturn(Map.of());

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    class EdgeCases {

        @Test
        @DisplayName("TC8: Пустой список на входе")
        void shouldReturnEmptyForEmptyInput() {
            List<Notification> result = service.getFilteredNotifications(List.of(), SENDER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(preferenceSettingsProvider, historyNotificationsProvider);
        }

        @Test
        @DisplayName("TC8: Null на входе")
        void shouldReturnEmptyForNullInput() {
            List<Notification> result = service.getFilteredNotifications(null, SENDER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(preferenceSettingsProvider, historyNotificationsProvider);
        }

        @Test
        @DisplayName("TC9: Отсутствие истории у пользователя")
        void shouldPassWhenNoHistory() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                    .thenReturn(Map.of(RECEIVER_ID, createPreferences(true, Set.of(NotificationType.EMAIL), Set.of())));
            when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class)))
                    .thenReturn(Map.of());

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("TC10: Смешанный батч")
        void shouldHandleMixedBatch() {
            Notification valid = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            Notification wrongChannel = createNotification(2, NotificationType.SMS, RECEIVER_ID, SENDER_ID);
            Notification duplicate = createNotification(3, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);

            when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                    .thenReturn(Map.of(RECEIVER_ID, createPreferences(true, Set.of(NotificationType.EMAIL), Set.of())));
            when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class)))
                    .thenReturn(Map.of(RECEIVER_ID, Set.of(3)));

            List<Notification> result = service.getFilteredNotifications(List.of(valid, wrongChannel, duplicate), SENDER_ID);

            assertEquals(1, result.size());
            assertTrue(result.contains(valid));
        }

        @Test
        @DisplayName("TC11: Возвращаемый список неизменяем")
        void shouldReturnUnmodifiableList() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            setupAllAllowed();

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertThrows(UnsupportedOperationException.class, () ->
                    result.add(createNotification(99, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID))
            );
        }

        @Test
        @DisplayName("TC13: notificationsEnabled=false имеет приоритет над allowedChannels")
        void shouldFilterAllEvenIfChannelsAllowed() {
            Notification notification = createNotification(1, NotificationType.EMAIL, RECEIVER_ID, SENDER_ID);
            when(preferenceSettingsProvider.getPreferencesByIds(anySet()))
                    .thenReturn(Map.of(RECEIVER_ID, createPreferences(false, Set.of(NotificationType.EMAIL), Set.of())));
            when(historyNotificationsProvider.getSentNotificationIds(anySet(), any(Instant.class))).thenReturn(Map.of());

            List<Notification> result = service.getFilteredNotifications(List.of(notification), SENDER_ID);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Проверка вызовов провайдеров")
    class ProviderInteractionTests {

        @Test
        @DisplayName("Провайдеры должны вызываться ровно один раз")
        void shouldCallProvidersExactlyOnce() {
            Notification n1 = createNotification(1, NotificationType.EMAIL, 10, SENDER_ID);
            Notification n2 = createNotification(2, NotificationType.SMS, 20, SENDER_ID);
            Notification n3 = createNotification(3, NotificationType.PUSH, 10, SENDER_ID);
            setupAllAllowed();

            service.getFilteredNotifications(List.of(n1, n2, n3), SENDER_ID);

            verify(preferenceSettingsProvider, times(1)).getPreferencesByIds(Set.of(10, 20));
            verify(historyNotificationsProvider, times(1)).getSentNotificationIds(eq(Set.of(10, 20)), any(Instant.class));
        }
    }
}

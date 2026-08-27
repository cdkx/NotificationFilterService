package ru.eremin;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


public interface HistoryNotificationsProvider {

    /**
     * Возвращает id уведомлений, которые были отправлены начиная с момента since. Не сами уведомления, а только id
     * @param recipientIds коллекция идентификаторов пользователей
     * @param since граница временного окна
     * @return map recipientId → множество id уже отправленных уведомлений
     */
    Map<Integer, Set<Integer>> getSentNotificationIds(Collection<Integer> recipientIds, Instant since);
}

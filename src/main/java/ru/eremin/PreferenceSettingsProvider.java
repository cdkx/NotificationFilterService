package ru.eremin;

import java.util.Collection;
import java.util.Map;

public interface PreferenceSettingsProvider {

    /**
     *
     * @param recipientIds идентификаторы получателей
     * @return map recipientId → PreferenceSettings
     */
    Map<Integer, PreferenceSettings> getPreferencesByIds(Collection<Integer> recipientIds);
}

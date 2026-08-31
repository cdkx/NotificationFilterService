package ru.eremin;

import java.util.Collections;
import java.util.Set;

public record PreferenceSettings(
        boolean notificationsEnabled,
        Set<NotificationType> allowedChannels,
        Set<Integer> blockedSenders
) {
    public PreferenceSettings {
        allowedChannels = allowedChannels == null ? Collections.emptySet() : Set.copyOf(allowedChannels);
        blockedSenders = blockedSenders == null ? Collections.emptySet() : Set.copyOf(blockedSenders);
    }
}

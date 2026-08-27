package ru.eremin;

import java.util.Collections;
import java.util.Set;

public record PreferenceSettings(
        Set<NotificationType> allowedChannels,
        Set<Integer> blockedSenders
) {
    public PreferenceSettings(Set<NotificationType> allowedChannels, Set<Integer> blockedSenders) {
        this.allowedChannels = allowedChannels == null ? Collections.emptySet() : Set.copyOf(allowedChannels);
        this.blockedSenders = blockedSenders == null ? Collections.emptySet() : Set.copyOf(blockedSenders);
    }
}

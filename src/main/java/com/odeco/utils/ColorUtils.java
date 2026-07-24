package com.odeco.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public static Component color(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        if (input.contains("&")) {
            return LEGACY.deserialize(input);
        }
        return MINI_MESSAGE.deserialize(input);
    }

    public static String legacy(String input) {
        if (input == null || input.isEmpty()) return "";
        return LEGACY.serialize(MINI_MESSAGE.deserialize(input));
    }
}

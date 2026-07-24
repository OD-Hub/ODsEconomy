package com.odeco.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static Component color(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(input);
    }

    public static String legacy(String input) {
        if (input == null || input.isEmpty()) return "";
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().serialize(MINI_MESSAGE.deserialize(input));
    }
}

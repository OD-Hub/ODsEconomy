package com.odeco.listeners;

import com.odeco.ODEco;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

public class ChatListener implements Listener {

    private final ODEco plugin;

    public ChatListener(ODEco plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPaperChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (plugin.getChatInputManager().handleInput(player, message)) {
            event.setCancelled(true);
            event.viewers().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getChatInputManager().handleInput(player, event.getMessage())) {
            event.setCancelled(true);
            event.getRecipients().clear();
        }
    }
}

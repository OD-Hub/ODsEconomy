package com.odeco;

import com.odeco.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pendingTimestamps = new ConcurrentHashMap<>();

    private static final long TIMEOUT_MS = 60000;

    public ChatInputManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        requestInput(player, prompt, callback, null);
    }

    public void requestInput(Player player, String prompt, Consumer<String> callback, Runnable reopenAction) {
        player.closeInventory();
        player.sendMessage(ColorUtils.color(prompt));
        Consumer<String> wrappedCallback = input -> {
            callback.accept(input);
            if (reopenAction != null) {
                plugin.getServer().getScheduler().runTask(plugin, reopenAction);
            }
        };
        pending.put(player.getUniqueId(), wrappedCallback);
        pendingTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean handleInput(Player player, String message) {
        cleanup();
        UUID uuid = player.getUniqueId();
        Consumer<String> callback = pending.remove(uuid);
        pendingTimestamps.remove(uuid);
        if (callback != null) {
            try {
                callback.accept(message);
            } catch (Exception e) {
                player.sendMessage(ColorUtils.color("<red>An error occurred processing your input.</red>"));
            }
            return true;
        }
        return false;
    }

    public boolean hasPending(Player player) {
        cleanup();
        return pending.containsKey(player.getUniqueId());
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
        pendingTimestamps.remove(player.getUniqueId());
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        pendingTimestamps.entrySet().removeIf(e -> now - e.getValue() > TIMEOUT_MS);
        pending.keySet().retainAll(pendingTimestamps.keySet());
    }
}

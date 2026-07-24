package com.odeco.listeners;

import com.odeco.ODEco;
import com.odeco.economy.EconomyManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public class BalanceScoreboardListener {

    private final ODEco plugin;
    private final ScoreboardManager manager;
    private final Scoreboard scoreboard;
    private BukkitTask updateTask;
    private final Map<UUID, String> lastDisplay = new HashMap<>();

    private static final String OBJECTIVE_NAME = "ecobal";
    private static final String TEAM_PREFIX = "bal_";

    public BalanceScoreboardListener(ODEco plugin) {
        this.plugin = plugin;
        this.manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager.getNewScoreboard();

        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if (obj == null) {
            obj = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty());
        }
        obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
    }

    public void start() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 40L);
    }

    public void stop() {
        if (updateTask != null) updateTask.cancel();
    }

    private void update() {
        EconomyManager economy = plugin.getEconomyManager();
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if (obj == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);

            double balance = economy.getBalance(player.getUniqueId());
            String compact = economy.formatCompact(balance);

            if (lastDisplay.containsKey(player.getUniqueId()) && lastDisplay.get(player.getUniqueId()).equals(compact)) {
                continue;
            }
            lastDisplay.put(player.getUniqueId(), compact);

            String teamName = TEAM_PREFIX + player.getUniqueId().toString().substring(0, 8);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }

            net.kyori.adventure.text.Component suffixComponent = plugin.getMiniMessage().deserialize(" <green>[" + compact + "]</green>");
            team.suffix(suffixComponent);

            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        }

        Set<UUID> currentPlayers = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) currentPlayers.add(p.getUniqueId());
        lastDisplay.keySet().removeIf(uuid -> !currentPlayers.contains(uuid));
    }
}

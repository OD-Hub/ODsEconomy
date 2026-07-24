package com.odeco;

import com.odeco.economy.EconomyManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ODEcoExpansion extends PlaceholderExpansion {

    private final ODEco plugin;

    public ODEcoExpansion(ODEco plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "odeco";
    }

    @Override
    public String getAuthor() {
        return "OD";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        EconomyManager economy = plugin.getEconomyManager();
        if (params.equalsIgnoreCase("balance")) {
            if (player == null) return "0";
            return economy.format(economy.getBalance(player.getUniqueId()));
        }
        if (params.equalsIgnoreCase("balance_raw")) {
            if (player == null) return "0";
            return String.valueOf(economy.getBalance(player.getUniqueId()));
        }
        if (params.equalsIgnoreCase("currency_singular")) {
            return economy.getCurrencySingular();
        }
        if (params.equalsIgnoreCase("currency_plural")) {
            return economy.getCurrencyPlural();
        }
        if (params.equalsIgnoreCase("currency_symbol")) {
            return economy.getCurrencySymbol();
        }
        if (params.startsWith("baltop_name_")) {
            try {
                int pos = Integer.parseInt(params.substring("baltop_name_".length()));
                var top = economy.getTopBalances();
                if (pos < 1 || pos > top.size()) return "N/A";
                return plugin.getServer().getOfflinePlayer(top.get(pos - 1).getKey()).getName();
            } catch (Exception e) {
                return "N/A";
            }
        }
        if (params.startsWith("baltop_balance_")) {
            try {
                int pos = Integer.parseInt(params.substring("baltop_balance_".length()));
                var top = economy.getTopBalances();
                if (pos < 1 || pos > top.size()) return "0";
                return economy.format(top.get(pos - 1).getValue());
            } catch (Exception e) {
                return "0";
            }
        }
        return null;
    }
}

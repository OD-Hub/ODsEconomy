package com.odeco.gui;

import com.odeco.ODEco;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GUIClickListener implements Listener {

    private final ODEco plugin;

    public GUIClickListener(ODEco plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Object holder = event.getInventory().getHolder();
        if (holder == null) return;
        if (event.getCurrentItem() == null) return;
        event.setCancelled(true);

        if (holder instanceof EcoPanelGUI) {
            ((EcoPanelGUI) holder).handleClick(event);
        } else if (holder instanceof AdminPanelGUI) {
            ((AdminPanelGUI) holder).handleClick(event);
        } else if (holder instanceof SharedAccountListGUI) {
            ((SharedAccountListGUI) holder).handleClick(event);
        } else if (holder instanceof SharedAccountGUI) {
            ((SharedAccountGUI) holder).handleClick(event);
        } else if (holder instanceof SharedAccountMembersGUI) {
            ((SharedAccountMembersGUI) holder).handleClick(event);
        } else if (holder instanceof AuctionHouseGUI) {
            ((AuctionHouseGUI) holder).handleClick(event);
        } else if (holder instanceof AuctionManageGUI) {
            ((AuctionManageGUI) holder).handleClick(event);
        } else if (holder instanceof BountyGUI) {
            ((BountyGUI) holder).handleClick(event);
        } else if (holder instanceof BountyManagerGUI) {
            ((BountyManagerGUI) holder).handleClick(event);
        } else if (holder instanceof TransactionGUI) {
            ((TransactionGUI) holder).handleClick(event);
        } else if (holder instanceof TaxManagerGUI) {
            ((TaxManagerGUI) holder).handleClick(event);
        } else if (holder instanceof TaxSetupWizardGUI) {
            ((TaxSetupWizardGUI) holder).handleClick(event);
        } else if (holder instanceof DealershipGUI) {
            ((DealershipGUI) holder).handleClick(event);
        } else if (holder instanceof DealershipSetupGUI) {
            ((DealershipSetupGUI) holder).handleClick(event);
        } else if (holder instanceof ItemSellSetupGUI) {
            ((ItemSellSetupGUI) holder).handleClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getInventory().getHolder();
        if (holder instanceof EcoPanelGUI
                || holder instanceof AdminPanelGUI
                || holder instanceof SharedAccountListGUI
                || holder instanceof SharedAccountGUI
                || holder instanceof SharedAccountMembersGUI
                || holder instanceof AuctionHouseGUI
                || holder instanceof AuctionManageGUI
                || holder instanceof BountyGUI
                || holder instanceof BountyManagerGUI
                || holder instanceof TransactionGUI
                || holder instanceof TaxManagerGUI
                || holder instanceof TaxSetupWizardGUI
                || holder instanceof DealershipGUI
                || holder instanceof DealershipSetupGUI
                || holder instanceof ItemSellSetupGUI) {
            event.setCancelled(true);
        }
    }
}

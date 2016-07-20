package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedInventory;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 7/21/16.
 */
public class PlayerGameModeListener extends DeltaEssentialsListener
{
    public PlayerGameModeListener(DeltaEssentials plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();
        GameMode currentMode = player.getGameMode();
        GameMode newMode = event.getNewGameMode();
        DeltaEssPlayerData playerData = plugin.getPlayerMap().get(name);
        String gameModePerm = "DeltaEss.GameMode." + newMode.name();

        // Ignore game mode changes to the same mode
        if(currentMode == event.getNewGameMode()) return;

        // Ignore if DeltaEssPlayer does not exist
        if(playerData == null) return;

        // Prevent game mode change during loading or saving
        if(plugin.getPlayerLockManager().isLocked(name))
        {
            player.sendMessage(Settings.format("PlayerLocked"));
            event.setCancelled(true);
            return;
        }

        // Check if gamemode is disabled or not the forced gamemode (if enabled)
        if(Settings.isGameModeBlocked(newMode) && !player.hasPermission(gameModePerm))
        {
            player.sendMessage(Settings.format("NoPermission", gameModePerm));
            event.setCancelled(true);
            return;
        }

        // Ignore players that have the single inventory permission
        if(player.hasPermission("DeltaEss.SharedGameModeInv")) return;

        // Save the inventory associated with the old game mode
        if(currentMode == GameMode.SURVIVAL)
        {
            playerData.setSurvival(new SavedInventory(player));
        }
        else if(currentMode == GameMode.CREATIVE)
        {
            playerData.setCreative(new SavedInventory(player));
        }

        // Apply the inventory associated with the new game mode
        if(newMode == GameMode.SURVIVAL)
        {
            SavedInventory savedSurvival = playerData.getSurvival();

            player.getInventory().setContents(savedSurvival.getContents());
            player.getInventory().setArmorContents(savedSurvival.getArmor());
            playerData.setSurvival(null);
        }
        else if(newMode == GameMode.CREATIVE)
        {
            SavedInventory savedCreative = playerData.getCreative();

            player.getInventory().setContents(savedCreative.getContents());
            player.getInventory().setArmorContents(savedCreative.getArmor());
            playerData.setCreative(null);
        }
        else
        {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
        }
    }
}

/*
 * This file is part of DeltaEssentials.
 *
 * DeltaEssentials is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaEssentials is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaEssentials.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerLoadedEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerPreLoadEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerPreSaveEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerSavedEvent;
import com.gmail.tracebachi.DeltaEssentials.Runnables.PlayerLoad;
import com.gmail.tracebachi.DeltaEssentials.Runnables.PlayerSave;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerEntry;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerStats;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedInventory;
import com.gmail.tracebachi.DeltaEssentials.Utils.xAuthUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.luricos.bukkit.xAuth.event.command.player.xAuthCommandLoginEvent;
import de.luricos.bukkit.xAuth.event.command.player.xAuthCommandRegisterEvent;
import de.luricos.bukkit.xAuth.event.player.xAuthPlayerJoinEvent;
import de.luricos.bukkit.xAuth.xAuthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerDataIOListener extends DeltaEssentialsListener
{
    public PlayerDataIOListener(DeltaEssentials plugin)
    {
        super(plugin);
    }

    @Override
    public void shutdown()
    {
        super.shutdown();
    }

    /**************************************************************************
     * Login Events
     *************************************************************************/

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommandLoginEvent(xAuthCommandLoginEvent event)
    {
        if(event.getStatus() != xAuthPlayer.Status.AUTHENTICATED) return;

        String name = event.getPlayerName();
        Player player = Bukkit.getPlayer(name);

        if(player == null) return;

        if(plugin.getPlayerMap().containsKey(name)) return;

        loadPlayer(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoinEvent(xAuthPlayerJoinEvent event)
    {
        if(event.getStatus() != xAuthPlayer.Status.AUTHENTICATED) return;

        String name = event.getPlayerName();
        Player player = Bukkit.getPlayer(name);

        if(player == null) return;

        if(plugin.getPlayerMap().containsKey(name)) return;

        loadPlayer(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerRegisterEvent(xAuthCommandRegisterEvent event)
    {
        String name = xAuthUtil.getPlayerNameFromRegisterEvent(event);

        if(name == null) return;

        if(event.getAction() != xAuthCommandRegisterEvent.Action.PLAYER_REGISTERED) return;

        Player player = Bukkit.getPlayer(name);

        if(player == null) return;

        if(plugin.getPlayerMap().containsKey(name)) return;

        loadPlayer(player);
    }

    /**************************************************************************
     * Logout Event
     *************************************************************************/

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();
        Long switchTime = plugin.getPlayerLockListener().remove(name);

        // TODO Make the time configurable?
        if(switchTime == null || (System.currentTimeMillis() - switchTime) > 2000)
        {
            if(plugin.getPlayerMap().containsKey(name))
            {
                savePlayer(player, null);
            }
        }

        plugin.getPlayerLockListener().remove(name);
        plugin.getPlayerMap().remove(name);
    }

    /**************************************************************************
     * GameModeChange Event
     *************************************************************************/

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();
        GameMode originalMode = player.getGameMode();
        GameMode newMode = event.getNewGameMode();
        DeltaEssPlayerData playerData = plugin.getPlayerMap().get(name);
        String gameModeBypassPerm = "DeltaEss.DisabledGameModeBypass." + newMode.name();

        // Ignore game mode changes to the same mode
        if(originalMode == event.getNewGameMode()) return;

        // Ignore if DeltaEssPlayer does not exist
        if(playerData == null) return;

        // Check if the game mode is disabled
        if(Settings.isGameModeDisabled(newMode) && !player.hasPermission(gameModeBypassPerm))
        {
            player.sendMessage(Settings.format("NoPermission", gameModeBypassPerm));
            event.setCancelled(true);
            return;
        }

        // Prevent game mode change during saves
        if(plugin.getPlayerLockListener().isLocked(name))
        {
            player.sendMessage(Settings.format("PlayerLocked"));
            event.setCancelled(true);
            return;
        }

        // Prevent game mode changes unless there is no forced game mode or player is exempt
        if(Settings.isDefaultGameModeForced() &&
            newMode != Settings.getDefaultGameMode() &&
            !player.hasPermission("DeltaEss.ForcedGameModeBypass"))
        {
            String defaultMode = Settings.getDefaultGameMode().name();

            player.sendMessage(Settings.format("GameModeBeingForced", defaultMode));
            event.setCancelled(true);
            return;
        }

        // Ignore players that have the single inventory permission
        if(player.hasPermission("DeltaEss.SingleInventory")) return;

        // Save the inventory associated with the old game mode
        if(originalMode == GameMode.SURVIVAL)
        {
            playerData.setSurvival(new SavedInventory(player));
        }
        else if(originalMode == GameMode.CREATIVE)
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

    /**************************************************************************
     * Player Loading / Saving Handing Methods
     *************************************************************************/

    public void onPlayerLoadSuccess(String name, PlayerEntry entry)
    {
        Player player = Bukkit.getPlayer(name);

        if(player == null) return;

        DeltaEssPlayerData playerData = new DeltaEssPlayerData();

        plugin.getPlayerLockListener().remove(name);
        plugin.getPlayerMap().put(name, playerData);

        applyPlayerEntry(player, entry, playerData);

        PlayerLoadedEvent loadedEvent = new PlayerLoadedEvent(player, entry.getMetaData());
        Bukkit.getPluginManager().callEvent(loadedEvent);
    }

    public void onPlayerNotFound(String name)
    {
        Player player = Bukkit.getPlayer(name);

        if(player == null) return;

        DeltaEssPlayerData playerData = new DeltaEssPlayerData();

        plugin.getPlayerLockListener().remove(name);
        plugin.getPlayerMap().put(name, playerData);

        GameMode defaultGameMode = Settings.getDefaultGameMode();

        if(player.getGameMode() != defaultGameMode)
        {
            player.setGameMode(defaultGameMode);
        }

        PlayerLoadedEvent event = new PlayerLoadedEvent(player);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void onPlayerLoadException(String name)
    {
        Player player = Bukkit.getPlayer(name);

        if(player == null) return;

        player.sendMessage(Prefixes.FAILURE + "Failed to load inventory.");
        player.sendMessage(Prefixes.FAILURE + "Refer to the console for more details.");
    }

    public void onPlayerSaveSuccess(String name, String destServer)
    {
        Player player = Bukkit.getPlayer(name);

        if(player == null) return;

        PlayerSavedEvent savedEvent = new PlayerSavedEvent(name, player);
        Bukkit.getPluginManager().callEvent(savedEvent);

        if(destServer == null)
        {
            plugin.getPlayerLockListener().remove(name);
            return;
        }

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Connect");
        output.writeUTF(destServer);
        player.sendPluginMessage(plugin, "BungeeCord", output.toByteArray());

        plugin.getPlayerLockListener().add(name, System.currentTimeMillis() + 15000);
    }

    public void onPlayerSaveException(String name)
    {
        Player player = Bukkit.getPlayer(name);

        if(player == null) return;

        player.sendMessage(Prefixes.FAILURE + "Failed to save inventory.");
        player.sendMessage(Prefixes.FAILURE + "Refer to the console for more details.");
    }

    /**************************************************************************
     * Player Loading / Saving Scheduling Methods
     *************************************************************************/

    public void loadPlayer(Player player)
    {
        Preconditions.checkNotNull(player);

        PlayerPreLoadEvent preLoadEvent = new PlayerPreLoadEvent(player);
        Bukkit.getPluginManager().callEvent(preLoadEvent);

        String name = player.getName();
        PlayerLoad runnable = new PlayerLoad(name, this, plugin);

        plugin.getPlayerLockListener().add(name);

        plugin.debug("Scheduling async player data load for {name:" + name + "}" );
        plugin.scheduleTaskAsync(runnable);
    }

    public void savePlayer(Player player, String destServer)
    {
        Preconditions.checkNotNull(player);

        Settings.runPreSaveCommands(player);

        PlayerPreSaveEvent preSaveEvent = new PlayerPreSaveEvent(player);
        Bukkit.getPluginManager().callEvent(preSaveEvent);

        String name = player.getName();
        DeltaEssPlayerData playerData = plugin.getPlayerMap().get(name);
        PlayerEntry entry = buildPlayerEntry(player, playerData, preSaveEvent.getMetaData());
        PlayerSave runnable = new PlayerSave(entry, destServer, this, plugin);

        plugin.getPlayerLockListener().add(name);

        plugin.debug("Scheduling async player data save for {name:" + name + "}");
        plugin.scheduleTaskAsync(runnable);
    }

    /**************************************************************************
     * Private Methods
     *************************************************************************/

    private PlayerEntry buildPlayerEntry(Player player, DeltaEssPlayerData playerData, ConfigurationSection metadata)
    {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(playerData);
        Preconditions.checkNotNull(metadata);

        PlayerStats playerStats = new PlayerStats(player);
        PlayerEntry entry = new PlayerEntry(player.getName());
        SavedInventory inventory = new SavedInventory(player);

        if(!Settings.canLoadAndSavePotionEffects())
        {
            playerStats.setPotionEffects(null);
        }

        if(player.hasPermission("DeltaEss.SingleInventory"))
        {
            playerData.setSurvival(inventory);
            playerData.setCreative(SavedInventory.EMPTY);
        }
        else if(player.getGameMode() == GameMode.SURVIVAL)
        {
            playerData.setSurvival(inventory);
        }
        else if(player.getGameMode() == GameMode.CREATIVE)
        {
            playerData.setCreative(inventory);
        }

        entry.setPlayerStats(playerStats);
        entry.setDeltaEssPlayerData(playerData);
        entry.setMetaData(metadata);

        return entry;
    }

    private void applyPlayerEntry(Player player, PlayerEntry entry, DeltaEssPlayerData playerData)
    {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(playerData);

        PlayerStats playerStats = entry.getPlayerStats();
        DeltaEssPlayerData dataFromEntry = entry.getDeltaEssPlayerData();

        player.setHealth(playerStats.getHealth());
        player.setFoodLevel(playerStats.getFoodLevel());
        player.setLevel(playerStats.getXpLevel());
        player.setExp(playerStats.getXpProgress());
        player.getEnderChest().setContents(playerStats.getEnderChest());

        if(Settings.canLoadAndSavePotionEffects())
        {
            for(PotionEffect effect : player.getActivePotionEffects())
            {
                player.removePotionEffect(effect.getType());
            }

            for(PotionEffect effect : playerStats.getPotionEffects())
            {
                player.addPotionEffect(effect);
            }
        }

        playerData.setSurvival(dataFromEntry.getSurvival());
        playerData.setCreative(dataFromEntry.getCreative());
        playerData.setSocialSpyEnabled(dataFromEntry.isSocialSpyEnabled());
        playerData.setTeleportDenyEnabled(dataFromEntry.isTeleportDenyEnabled());
        playerData.setReplyTo(dataFromEntry.getReplyTo());
        playerData.setVanishEnabled(dataFromEntry.isVanishEnabled());

        if(player.hasPermission("DeltaEss.SingleInventory"))
        {
            SavedInventory survival = playerData.getSurvival();

            player.getInventory().setContents(survival.getContents());
            player.getInventory().setArmorContents(survival.getArmor());
            playerData.setSurvival(null);
            playerData.setCreative(null);
        }
        else if(player.getGameMode() == GameMode.SURVIVAL)
        {
            SavedInventory survival = playerData.getSurvival();

            player.getInventory().setContents(survival.getContents());
            player.getInventory().setArmorContents(survival.getArmor());
            playerData.setSurvival(null);
        }
        else if(player.getGameMode() == GameMode.CREATIVE)
        {
            SavedInventory creative = playerData.getCreative();

            player.getInventory().setContents(creative.getContents());
            player.getInventory().setArmorContents(creative.getArmor());
            playerData.setCreative(null);
        }
        else
        {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
        }

        GameMode defaultGameMode = Settings.getDefaultGameMode();
        boolean bypassForced = player.hasPermission("DeltaEss.Forced.Bypass");

        if(bypassForced || !Settings.isDefaultGameModeForced())
        {
            if(player.getGameMode() != playerStats.getGameMode())
            {
                player.setGameMode(playerStats.getGameMode());
            }
        }
        else if(player.getGameMode() != defaultGameMode)
        {
            player.setGameMode(defaultGameMode);
        }
    }
}

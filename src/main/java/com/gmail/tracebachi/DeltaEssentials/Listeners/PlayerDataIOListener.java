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
import com.gmail.tracebachi.DeltaEssentials.Events.*;
import com.gmail.tracebachi.DeltaEssentials.Runnables.PlayerLoad;
import com.gmail.tracebachi.DeltaEssentials.Runnables.PlayerSave;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerEntry;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerStats;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedInventory;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Map;

import static com.gmail.tracebachi.DeltaRedis.Shared.Prefixes.FAILURE;

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
     * Login, Logout, and Load Request Events
     *************************************************************************/

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if(Settings.shouldLoadPlayerDataOnLogin())
        {
            Player player = event.getPlayer();
            PlayerLoadRequestEvent requestEvent = new PlayerLoadRequestEvent(player);

            Bukkit.getPluginManager().callEvent(requestEvent);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();
        Map<String, DeltaEssPlayerData> playerMap = plugin.getPlayerMap();
        PlayerLockManager lockManager = plugin.getPlayerLockManager();

        if(playerMap.containsKey(name) && !lockManager.isLocked(name))
        {
            savePlayer(player);
        }

        lockManager.remove(name);
        playerMap.remove(name);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLoadRequest(PlayerLoadRequestEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();

        if(plugin.getPlayerMap().containsKey(name))
        {
            throw new IllegalArgumentException("Player {name: " +
                name + "} has already been loaded.");
        }

        loadPlayer(player);
    }

    /**************************************************************************
     * Player Loading / Saving Scheduling Methods
     *************************************************************************/

    public void loadPlayer(Player player)
    {
        PlayerPreLoadEvent preLoadEvent = new PlayerPreLoadEvent(player);
        Bukkit.getPluginManager().callEvent(preLoadEvent);

        String name = player.getName();
        PlayerLoad runnable = new PlayerLoad(name, this, plugin);

        plugin.getPlayerLockManager().add(name);
        plugin.scheduleTaskAsync(runnable, "PlayerLoad for {name: " + name + "}");
    }

    public void savePlayer(Player player)
    {
        saveAndMovePlayer(player, null);
    }

    public void saveAndMovePlayer(Player player, String destServer)
    {
        Settings.runPreSaveCommands(player);

        String name = player.getName();
        DeltaEssPlayerData playerData = plugin.getPlayerMap().get(name);

        PlayerPreSaveEvent preSaveEvent = new PlayerPreSaveEvent(
            player,
            playerData.getMetaData());
        Bukkit.getPluginManager().callEvent(preSaveEvent);

        PlayerEntry entry = buildPlayerEntry(player, playerData);
        PlayerSave runnable = new PlayerSave(entry, destServer, this, plugin);

        plugin.getPlayerLockManager().add(name);
        plugin.scheduleTaskAsync(runnable, "PlayerSave for {name: " + name + "}");
    }

    /**************************************************************************
     * Player Loading / Saving Handing Methods
     *************************************************************************/

    public void onPlayerLoadSuccess(String name, PlayerEntry entry)
    {
        Player player = Bukkit.getPlayerExact(name);
        PlayerStats playerStats = entry.getPlayerStats();
        DeltaEssPlayerData playerData = entry.getDeltaEssPlayerData();

        if(player == null) { return; }

        plugin.getPlayerLockManager().remove(name);
        plugin.getPlayerMap().put(name, playerData);

        applyEntryToPlayer(playerStats, playerData, player);

        PlayerPostLoadEvent loadedEvent = new PlayerPostLoadEvent(
            player,
            playerData.getMetaData(),
            false);
        Bukkit.getPluginManager().callEvent(loadedEvent);
    }

    public void onPlayerNotFound(String name)
    {
        Player player = Bukkit.getPlayerExact(name);
        DeltaEssPlayerData playerData = new DeltaEssPlayerData();
        GameMode defaultGameMode = Settings.getDefaultGameMode();

        if(player == null) { return; }

        plugin.getPlayerLockManager().remove(name);
        plugin.getPlayerMap().put(name, playerData);

        if(player.getGameMode() != defaultGameMode)
        {
            player.setGameMode(defaultGameMode);
        }

        PlayerPostLoadEvent loadedEvent = new PlayerPostLoadEvent(
            player,
            new YamlConfiguration(),
            true);
        Bukkit.getPluginManager().callEvent(loadedEvent);
    }

    public void onPlayerLoadException(String name)
    {
        Player player = Bukkit.getPlayerExact(name);

        if(player == null) { return; }

        player.sendMessage(FAILURE + "Failed to load inventory. " +
            "Refer to the console for more details.");
    }

    public void onPlayerSaveSuccess(String name, String destServer)
    {
        PlayerPostSaveEvent savedEvent = new PlayerPostSaveEvent(name);
        Bukkit.getPluginManager().callEvent(savedEvent);

        Player player = Bukkit.getPlayerExact(name);

        if(player == null) { return; }

        if(destServer == null) { return; }

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Connect");
        output.writeUTF(destServer);
        player.sendPluginMessage(plugin, "BungeeCord", output.toByteArray());

        plugin.getPlayerLockManager().add(
            name,
            System.currentTimeMillis() + 15000); // TODO Make configurable time
    }

    public void onPlayerSaveException(String name)
    {
        Player player = Bukkit.getPlayerExact(name);

        if(player == null) { return; }

        player.sendMessage(FAILURE + "Failed to save inventory. " +
            "Refer to the console for more details.");
    }

    /**************************************************************************
     * Private Methods
     *************************************************************************/

    private void applyEntryToPlayer(PlayerStats playerStats, DeltaEssPlayerData playerData,
                                    Player player)
    {
        Preconditions.checkNotNull(player, "Player was null.");
        Preconditions.checkNotNull(playerStats, "PlayerStats was null.");
        Preconditions.checkNotNull(playerData, "PlayerData was null.");

        GameMode defaultMode = Settings.getDefaultGameMode();

        player.setHealth(playerStats.getHealth());
        player.setFoodLevel(playerStats.getFoodLevel());
        player.setLevel(playerStats.getXpLevel());
        player.setExp(playerStats.getXpProgress());
        player.getEnderChest().setContents(playerStats.getEnderChest());

        if(!Settings.shouldIgnorePotionEffects())
        {
            for(PotionEffect effect : player.getActivePotionEffects())
            {
                player.removePotionEffect(effect.getType());
            }

            for(PotionEffect effect : playerData.getPotionEffects())
            {
                player.addPotionEffect(effect);
            }
        }

        if(player.hasPermission("DeltaEss.SharedGameModeInv"))
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

        if(!Settings.isDefaultGameModeForced() ||
            player.hasPermission("DeltaEss.GameMode." + defaultMode))
        {
            if(player.getGameMode() != playerStats.getGameMode())
            {
                player.setGameMode(playerStats.getGameMode());
            }
        }
        else if(player.getGameMode() != defaultMode)
        {
            player.setGameMode(defaultMode);
        }
    }

    private PlayerEntry buildPlayerEntry(Player player, DeltaEssPlayerData playerData)
    {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(playerData);

        PlayerStats playerStats = new PlayerStats(player);
        PlayerEntry entry = new PlayerEntry(player.getName());
        SavedInventory inventory = new SavedInventory(player);

        if(!Settings.shouldIgnorePotionEffects())
        {
            playerData.setPotionEffects(player.getActivePotionEffects());
        }

        if(player.hasPermission("DeltaEss.SharedGameModeInv"))
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

        return entry;
    }
}

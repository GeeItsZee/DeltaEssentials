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
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerLoadRequestEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerPostLoadEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerPreLoadEvent;
import com.gmail.tracebachi.DeltaEssentials.Runnables.PlayerLoad;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedPlayerInventory;
import com.gmail.tracebachi.DeltaEssentials.Utils.DeltaEssPlayerDataHelper;
import com.gmail.tracebachi.DeltaExecutor.DeltaExecutor;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerDataLoadListener extends DeltaEssentialsListener
{
    private LockedPlayerManager lockedPlayerManager;
    private DeltaEssPlayerDataHelper playerDataHelper;
    private Settings settings;

    public PlayerDataLoadListener(LockedPlayerManager lockedPlayerManager,
                                  DeltaEssPlayerDataHelper playerDataHelper,
                                  Settings settings,
                                  DeltaEssentials plugin)
    {
        super(plugin);

        Preconditions.checkNotNull(lockedPlayerManager, "lockedPlayerManager");
        Preconditions.checkNotNull(playerDataHelper, "playerDataHelper");
        Preconditions.checkNotNull(settings, "settings");

        this.lockedPlayerManager = lockedPlayerManager;
        this.playerDataHelper = playerDataHelper;
        this.settings = settings;
    }

    @Override
    public void shutdown()
    {
        this.lockedPlayerManager = null;
        this.playerDataHelper = null;
        this.settings = null;
        super.shutdown();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if(settings.shouldLoadPlayerDataOnLogin())
        {
            Player player = event.getPlayer();
            PlayerLoadRequestEvent requestEvent = new PlayerLoadRequestEvent(player);

            Bukkit.getPluginManager().callEvent(requestEvent);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerLoadRequest(PlayerLoadRequestEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();

        plugin.debug("Received PlayerLoadRequest for name: " + name);

        if(!plugin.getPlayerDataMap().containsKey(name))
        {
            loadPlayer(player);
            return;
        }

        throw new IllegalArgumentException("Player: " + name + " already loaded");
    }

    private void loadPlayer(Player player)
    {
        Preconditions.checkNotNull(player, "player");

        PlayerPreLoadEvent preLoadEvent = new PlayerPreLoadEvent(player);
        Bukkit.getPluginManager().callEvent(preLoadEvent);

        String playerName = player.getName();
        lockedPlayerManager.add(playerName);

        plugin.debug("PlayerLoad with playerName: " + playerName);

        File playerDataFile = settings.getPlayerDataFileFor(playerName);
        PlayerLoad runnable = new PlayerLoad(
            playerName,
            playerDataFile,
            new PlayerLoadCallbacks(playerName));
        DeltaExecutor.instance().execute(runnable);
    }

    /**
     * Implementation of PlayerLoad.Callbacks specific to loading a player
     * from PlayerDataLoadListener
     */
    private class PlayerLoadCallbacks implements PlayerLoad.Callbacks
    {
        private String playerName;

        public PlayerLoadCallbacks(String playerName)
        {
            Preconditions.checkNotNull(playerName, "playerName");
            this.playerName = playerName;
        }

        @Override
        public void onSuccess(DeltaEssPlayerData playerData)
        {
            Bukkit.getScheduler().runTask(plugin, () ->
            {
                Player player = Bukkit.getPlayerExact(playerName);
                if(player != null)
                {
                    // Unlock the player
                    lockedPlayerManager.remove(playerName);

                    // Save the playerData
                    plugin.getPlayerDataMap().put(playerName, playerData);

                    // Apply the playerData
                    playerDataHelper.applyPlayerData(playerData, player);

                    // Fire a PlayerPostLoadEvent
                    PlayerPostLoadEvent postLoadEvent = new PlayerPostLoadEvent(
                        player,
                        playerData.getMetaData());
                    Bukkit.getPluginManager().callEvent(postLoadEvent);
                }
            });
        }

        @Override
        public void onNotFoundFailure()
        {
            Bukkit.getScheduler().runTask(plugin, () ->
            {
                Player player = Bukkit.getPlayerExact(playerName);
                if(player != null)
                {
                    // Unlock the player
                    lockedPlayerManager.remove(playerName);

                    // Build new playerData
                    DeltaEssPlayerData playerData = new DeltaEssPlayerData(playerName);
                    SavedPlayerInventory playerInventory = new SavedPlayerInventory(player);

                    // Set the gamemode as the default on the server
                    playerData.setGameMode(settings.getDefaultGameMode());

                    // Save the current EnderChest
                    playerData.setEnderChest(player.getEnderChest().getContents());

                    // Save the current inventory based on GameMode
                    if(player.getGameMode() == GameMode.SURVIVAL)
                    {
                        playerData.setSurvival(playerInventory);
                    }
                    else if(player.getGameMode() == GameMode.CREATIVE)
                    {
                        playerData.setCreative(playerInventory);
                    }

                    // Save the playerData
                    plugin.getPlayerDataMap().put(playerName, playerData);

                    // Apply the playerData
                    playerDataHelper.applyPlayerData(playerData, player);

                    PlayerPostLoadEvent postLoadEvent = new PlayerPostLoadEvent(
                        player,
                        playerData.getMetaData(),
                        true);
                    Bukkit.getPluginManager().callEvent(postLoadEvent);
                }
            });
        }

        @Override
        public void onExceptionFailure(Exception ex)
        {
            ex.printStackTrace();

            Bukkit.getScheduler().runTask(plugin, () ->
            {
                Player player = Bukkit.getPlayerExact(playerName);
                if(player != null)
                {
                    lockedPlayerManager.remove(playerName);

                    player.sendMessage(format("DeltaEss.FailedToLoadInventory"));
                }
            });
        }
    }
}

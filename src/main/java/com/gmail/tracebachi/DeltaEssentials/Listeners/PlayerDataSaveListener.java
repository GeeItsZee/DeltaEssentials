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
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerPostSaveEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerPreSaveEvent;
import com.gmail.tracebachi.DeltaEssentials.Runnables.PlayerSave;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Utils.DeltaEssPlayerDataHelper;
import com.gmail.tracebachi.DeltaExecutor.DeltaExecutor;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.Map;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/23/16.
 */
public class PlayerDataSaveListener extends DeltaEssentialsListener
{
    private LockedPlayerManager lockedPlayerManager;
    private DeltaEssPlayerDataHelper playerDataHelper;
    private Settings settings;

    public PlayerDataSaveListener(LockedPlayerManager lockedPlayerManager,
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        String playerName = player.getName();
        Map<String, DeltaEssPlayerData> playerDataMap = plugin.getPlayerDataMap();
        DeltaEssPlayerData playerData = playerDataMap.get(playerName);

        if(playerData != null && !lockedPlayerManager.isLocked(playerName))
        {
            savePlayer(player);
        }

        lockedPlayerManager.remove(playerName);
        playerDataMap.remove(playerName);
    }

    public void savePlayer(Player player)
    {
        saveAndMovePlayer(player, "");
    }

    public void saveAndMovePlayer(Player player, String destServer)
    {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(destServer, "destServer");

        // Run the pre-save commands as the player
        settings.getPreSaveCommands().forEach(player::performCommand);

        String playerName = player.getName();
        DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(playerName);

        // Lock the player
        lockedPlayerManager.add(playerName);

        // Update the player data with the latest player info
        playerDataHelper.updatePlayerData(playerData, player);

        PlayerPreSaveEvent preSaveEvent = new PlayerPreSaveEvent(
            player,
            playerData.getMetaData());
        Bukkit.getPluginManager().callEvent(preSaveEvent);

        plugin.debug("PlayerSave with playerName: " + playerName +
            ", destServer: " + destServer);

        File playerDataFile = settings.getPlayerDataFileFor(playerName);
        PlayerSave runnable = new PlayerSave(
            playerData,
            playerDataFile,
            new PlayerSaveCallbacks(playerName, destServer));
        DeltaExecutor.instance().execute(runnable);
    }

    private class PlayerSaveCallbacks implements PlayerSave.Callbacks
    {
        private final String playerName;
        private final String destServer;

        public PlayerSaveCallbacks(String playerName, String destServer)
        {
            Preconditions.checkNotNull(playerName, "playerName");
            Preconditions.checkArgument(!playerName.isEmpty(), "Empty playerName");
            Preconditions.checkNotNull(destServer, "destServer");

            this.playerName = playerName;
            this.destServer = destServer;
        }

        @Override
        public void onSuccess()
        {
            Bukkit.getScheduler().runTask(plugin, () ->
            {
                PlayerPostSaveEvent savedEvent = new PlayerPostSaveEvent(playerName);
                Bukkit.getPluginManager().callEvent(savedEvent);

                Player player = Bukkit.getPlayerExact(playerName);
                if(player != null && !destServer.isEmpty())
                {
                    ByteArrayDataOutput output = ByteStreams.newDataOutput();
                    output.writeUTF("Connect");
                    output.writeUTF(destServer);
                    player.sendPluginMessage(plugin, "BungeeCord", output.toByteArray());

                    lockedPlayerManager.add(playerName, System.currentTimeMillis() + 15000);
                }
            });
        }

        @Override
        public void onFailure(Exception ex)
        {
            if(ex != null) { ex.printStackTrace(); }

            Bukkit.getScheduler().runTask(plugin, () ->
            {
                Player player = Bukkit.getPlayerExact(playerName);
                if(player != null)
                {
                    player.sendMessage(format("DeltaEss.FailedToSaveInventory"));

                    lockedPlayerManager.remove(playerName);
                }
            });
        }
    }
}

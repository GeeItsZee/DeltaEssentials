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
package com.gmail.tracebachi.DeltaEssentials;

import com.gmail.tracebachi.DeltaEssentials.Commands.*;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerServerSwitchEvent;
import com.gmail.tracebachi.DeltaEssentials.Listeners.*;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Utils.DeltaEssPlayerDataHelper;
import com.gmail.tracebachi.DeltaExecutor.DeltaExecutor;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/5/15.
 */
public class DeltaEssentials extends JavaPlugin
{
    private Settings settings;
    private DeltaEssPlayerDataHelper playerDataHelper;
    private CaseInsensitiveHashMap<DeltaEssPlayerData> playerMap = new CaseInsensitiveHashMap<>();

    private LockedPlayerManager lockedPlayerManager;
    private PlayerDataLoadListener playerDataLoadListener;
    private PlayerDataSaveListener playerDataSaveListener;
    private PlayerGameModeListener playerGameModeListener;
    private SharedChatListener sharedChatListener;
    private TeleportListener teleportListener;

    private CommandDeltaEss commandDeltaEss;
    private CommandDisposal commandDisposal;
    private CommandDVanish commandDVanish;
    private CommandLockdown commandLockdown;
    private CommandMoveTo commandMoveTo;
    private CommandMoveAll commandMoveAll;
    private CommandSocialSpy commandSocialSpy;
    private CommandTell commandTell;
    private CommandTp commandTp;
    private CommandTpAccept commandTpAccept;
    private CommandTpaHere commandTpaHere;
    private CommandTpHere commandTpHere;
    private CommandTpDeny commandTpDeny;

    @Override
    public void onLoad()
    {
        saveDefaultConfig();
    }

    @Override
    public void onEnable()
    {
        reloadConfig();

        settings = new Settings();
        settings.read(getConfig());

        playerDataHelper = new DeltaEssPlayerDataHelper(settings);

        lockedPlayerManager = new LockedPlayerManager(this);
        lockedPlayerManager.register();

        playerDataLoadListener = new PlayerDataLoadListener(
            lockedPlayerManager,
            playerDataHelper,
            settings,
            this);
        playerDataLoadListener.register();

        playerDataSaveListener = new PlayerDataSaveListener(
            lockedPlayerManager,
            playerDataHelper,
            settings,
            this);
        playerDataSaveListener.register();

        playerGameModeListener = new PlayerGameModeListener(lockedPlayerManager, settings, this);
        playerGameModeListener.register();

        sharedChatListener = new SharedChatListener(this);
        sharedChatListener.register();

        teleportListener = new TeleportListener(this);
        teleportListener.register();

        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, "BungeeCord");

        // Add a player for the console so the console's reply targets are stored
        // TODO Special case handling for console
        // playerMap.put("console", new DeltaEssPlayerData());

        commandDeltaEss = new CommandDeltaEss(this);
        commandDeltaEss.register();

        commandDisposal = new CommandDisposal(this);
        commandDisposal.register();

        commandDVanish = new CommandDVanish(this);
        commandDVanish.register();

        commandLockdown = new CommandLockdown(this);
        commandLockdown.register();

        commandMoveTo = new CommandMoveTo(this);
        commandMoveTo.register();

        commandMoveAll = new CommandMoveAll(this);
        commandMoveAll.register();

        commandSocialSpy = new CommandSocialSpy(this);
        commandSocialSpy.register();

        commandTell = new CommandTell(this);
        commandTell.register();

        commandTp = new CommandTp(teleportListener, this);
        commandTp.register();

        commandTpAccept = new CommandTpAccept(teleportListener, this);
        commandTpAccept.register();

        commandTpaHere = new CommandTpaHere(teleportListener, this);
        commandTpaHere.register();

        commandTpHere = new CommandTpHere(teleportListener, this);
        commandTpHere.register();

        commandTpDeny = new CommandTpDeny(this);
        commandTpDeny.register();
    }

    @Override
    public void onDisable()
    {
        // Save player data for all players
        for(Player player : Bukkit.getOnlinePlayers())
        {
            try
            {
                playerDataSaveListener.savePlayer(player);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                severe("Failed to save player data on shutdown for " + player.getName());
            }
        }

        // Shutdown the async executor (will wait for all tasks to complete)
        DeltaExecutor.instance().shutdown();

        // Order matters
        // Shut down the Load/Save listeners before the inventory lock listener
        if(playerDataLoadListener != null)
        {
            playerDataLoadListener.shutdown();
            playerDataLoadListener = null;
        }

        if(playerDataSaveListener != null)
        {
            playerDataSaveListener.shutdown();
            playerDataSaveListener = null;
        }

        if(playerGameModeListener != null)
        {
            playerGameModeListener.shutdown();
            playerGameModeListener = null;
        }

        if(teleportListener != null)
        {
            teleportListener.shutdown();
            teleportListener = null;
        }

        if(sharedChatListener != null)
        {
            sharedChatListener.shutdown();
            sharedChatListener = null;
        }

        if(lockedPlayerManager != null)
        {
            lockedPlayerManager.shutdown();
            lockedPlayerManager = null;
        }

        commandDeltaEss.shutdown();
        commandDeltaEss = null;

        commandDisposal.shutdown();
        commandDisposal = null;

        commandDVanish.shutdown();
        commandDVanish = null;

        commandLockdown.shutdown();
        commandLockdown = null;

        commandMoveTo.shutdown();
        commandMoveTo = null;

        commandMoveAll.shutdown();
        commandMoveAll = null;

        commandSocialSpy.shutdown();
        commandSocialSpy = null;

        commandTell.shutdown();
        commandTell = null;

        commandTp.shutdown();
        commandTp = null;

        commandTpAccept.shutdown();
        commandTpAccept = null;

        commandTpaHere.shutdown();
        commandTpaHere = null;

        commandTpHere.shutdown();
        commandTpHere = null;

        commandTpDeny.shutdown();
        commandTpDeny = null;

        Messenger messenger = getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(this);
    }

    public void info(String message)
    {
        getLogger().info(message);
    }

    public void severe(String message)
    {
        getLogger().severe(message);
    }

    public void debug(String message)
    {
        if(settings.isDebugEnabled())
        {
            getLogger().info("[Debug] " + message);
        }
    }

    public Map<String, DeltaEssPlayerData> getPlayerDataMap()
    {
        return playerMap;
    }

    public boolean sendToServer(Player player, String destination)
    {
        return sendToServer(player, destination, true);
    }

    public boolean sendToServer(Player player, String destination, boolean callEvent)
    {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(destination, "destination");

        if(!playerMap.containsKey(player.getName())) { return false; }

        if(callEvent)
        {
            PlayerServerSwitchEvent event = new PlayerServerSwitchEvent(player, destination);
            Bukkit.getPluginManager().callEvent(event);

            if(event.isCancelled())
            {
                return false;
            }
        }

        playerDataSaveListener.saveAndMovePlayer(player, destination);
        return true;
    }
}

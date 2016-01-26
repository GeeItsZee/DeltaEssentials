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
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssentialsPlayer;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedis;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.WaitingAsyncScheduler;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/5/15.
 */
public class DeltaEssentials extends JavaPlugin
{
    private boolean debugMode;
    private boolean disableTaskScheduling;

    private Settings settings;
    private WaitingAsyncScheduler asyncScheduler;
    private CaseInsensitiveHashMap<DeltaEssentialsPlayer> playerMap = new CaseInsensitiveHashMap<>();

    private ChatListener chatListener;
    private PlayerLockListener playerLockListener;
    private PlayerDataIOListener playerDataIOListener;
    private TeleportListener teleportListener;

    private CommandDisposal commandDisposal;
    private CommandJail commandJail;
    private CommandKick commandKick;
    private CommandLockdown commandLockdown;
    private CommandMoveTo commandMoveTo;
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
        debugMode = getConfig().getBoolean("DebugMode", false);
        settings = new Settings(this);
        asyncScheduler = new WaitingAsyncScheduler(this);

        PluginManager pluginManager = getServer().getPluginManager();
        DeltaRedis deltaRedisPlugin = (DeltaRedis) pluginManager.getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = deltaRedisPlugin.getDeltaRedisApi();

        // Add a player for the console so the console's reply targets are stored
        playerMap.put("console", new DeltaEssentialsPlayer());

        commandDisposal = new CommandDisposal(this);
        commandDisposal.register();

        commandJail = new CommandJail(deltaRedisApi, this);
        commandJail.register();

        commandKick = new CommandKick(deltaRedisApi, this);
        commandKick.register();

        commandLockdown = new CommandLockdown(this);
        commandLockdown.register();

        commandMoveTo = new CommandMoveTo(deltaRedisApi, this);
        commandMoveTo.register();

        commandSocialSpy = new CommandSocialSpy(this);
        commandSocialSpy.register();

        commandTell = new CommandTell(deltaRedisApi, this);
        commandTell.register();

        commandTp = new CommandTp(deltaRedisApi, this);
        commandTp.register();

        commandTpAccept = new CommandTpAccept(deltaRedisApi, this);
        commandTpAccept.register();

        commandTpaHere = new CommandTpaHere(deltaRedisApi, this);
        commandTpaHere.register();

        commandTpHere = new CommandTpHere(deltaRedisApi, this);
        commandTpHere.register();

        commandTpDeny = new CommandTpDeny(this);
        commandTpDeny.register();

        chatListener = new ChatListener(deltaRedisApi, this);
        chatListener.register();

        teleportListener = new TeleportListener(deltaRedisApi, this);
        teleportListener.register();

        playerLockListener = new PlayerLockListener(this);
        playerLockListener.register();

        playerDataIOListener = new PlayerDataIOListener(this);
        playerDataIOListener.register();

        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, "BungeeCord");

        disableTaskScheduling = false;
    }

    @Override
    public void onDisable()
    {
        disableTaskScheduling = true;

        // Wait for any async tasks to finish
        asyncScheduler.waitForTasks();
        asyncScheduler = null;

        // Order matters. Shut down the IO listener before the inventory lock listener
        playerDataIOListener.shutdown();
        playerDataIOListener = null;

        playerLockListener.shutdown();
        playerLockListener = null;

        teleportListener.shutdown();
        teleportListener = null;

        chatListener.shutdown();
        chatListener = null;

        commandDisposal.shutdown();
        commandDisposal = null;

        commandJail.shutdown();
        commandJail = null;

        commandKick.shutdown();
        commandKick = null;

        commandLockdown.shutdown();
        commandLockdown = null;

        commandMoveTo.shutdown();
        commandMoveTo = null;

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
        if(debugMode)
        {
            getLogger().info("[Debug] " + message);
        }
    }

    public CaseInsensitiveHashMap<DeltaEssentialsPlayer> getPlayerMap()
    {
        return playerMap;
    }

    public ChatListener getChatListener()
    {
        return chatListener;
    }

    public PlayerDataIOListener getPlayerDataIOListener()
    {
        return playerDataIOListener;
    }

    public PlayerLockListener getPlayerLockListener()
    {
        return playerLockListener;
    }

    public TeleportListener getTeleportListener()
    {
        return teleportListener;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public void scheduleTaskSync(Runnable runnable)
    {
        if(!disableTaskScheduling)
        {
            getServer().getScheduler().runTask(this, runnable);
        }
        else
        {
            runnable.run();
        }
    }

    public void scheduleTaskAsync(Runnable runnable)
    {
        if(!disableTaskScheduling)
        {
            asyncScheduler.scheduleTask(runnable);
        }
        else
        {
            runnable.run();
        }
    }

    public boolean sendToServer(Player player, String destination)
    {
        return sendToServer(player, destination, true);
    }

    public boolean sendToServer(Player player, String destination, boolean callEvent)
    {
        Preconditions.checkNotNull(player, "Player cannot be null.");
        Preconditions.checkNotNull(destination, "Destination server cannot be null.");

        if(callEvent)
        {
            PlayerServerSwitchEvent event = new PlayerServerSwitchEvent(player, destination);
            Bukkit.getPluginManager().callEvent(event);

            if(event.isCancelled())
            {
                return false;
            }
        }

        playerDataIOListener.savePlayerData(player, destination);
        return true;
    }
}

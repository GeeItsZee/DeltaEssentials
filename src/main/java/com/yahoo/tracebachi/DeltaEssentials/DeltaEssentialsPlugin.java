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
package com.yahoo.tracebachi.DeltaEssentials;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaEssentials.Chat.DeltaChat;
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerServerSwitchEvent;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.DeltaTeleport;
import com.yahoo.tracebachi.DeltaRedis.Shared.Interfaces.LoggablePlugin;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/5/15.
 */
public class DeltaEssentialsPlugin extends JavaPlugin implements LoggablePlugin
{
    private boolean debugMode;
    private HashSet<String> blockedServers = new HashSet<>();
    private MoveToCommand moveToCommand;
    private KickCommand kickCommand;
    private KickListener kickListener;

    private DeltaChat deltaChat;
    private DeltaTeleport deltaTeleport;

    @Override
    public void onLoad()
    {
        File file = new File(getDataFolder(), "config.yml");
        if(!file.exists()) { saveDefaultConfig(); }
    }

    @Override
    public void onEnable()
    {
        reloadConfig();
        List<String> blockedServerList = getConfig().getStringList("blocked_servers");
        List<String> ignoredChannelList = getConfig().getStringList("ignored_channels");
        debugMode = getConfig().getBoolean("debug_mode", false);
        blockedServers.addAll(blockedServerList.stream().map(String::toLowerCase).collect(Collectors.toList()));

        PluginManager pluginManager = getServer().getPluginManager();
        DeltaRedisPlugin deltaRedisPlugin = (DeltaRedisPlugin) pluginManager.getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = deltaRedisPlugin.getDeltaRedisApi();

        moveToCommand = new MoveToCommand(this, deltaRedisApi);
        getCommand("moveto").setExecutor(moveToCommand);
        kickCommand = new KickCommand(deltaRedisApi);
        getCommand("kick").setExecutor(kickCommand);

        deltaChat = new DeltaChat(this, deltaRedisApi, ignoredChannelList);
        deltaTeleport = new DeltaTeleport(this, deltaRedisApi);

        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, "BungeeCord");

        kickListener = new KickListener();
        getServer().getPluginManager().registerEvents(kickListener, this);
    }

    @Override
    public void onDisable()
    {
        kickListener = null;

        Messenger messenger = getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(this);

        if(deltaTeleport != null)
        {
            deltaTeleport.shutdown();
            deltaTeleport = null;
        }

        if(deltaChat != null)
        {
            deltaChat.shutdown();
            deltaChat = null;
        }

        if(kickCommand != null)
        {
            getCommand("kick").setExecutor(null);
            kickCommand.shutdown();
            kickCommand = null;
        }

        if(moveToCommand != null)
        {
            getCommand("moveto").setExecutor(null);
            moveToCommand.shutdown();
            moveToCommand = null;
        }

        if(blockedServers != null)
        {
            blockedServers.clear();
            blockedServers = null;
        }
    }

    @Override
    public void info(String message)
    {
        getLogger().info(message);
    }

    @Override
    public void severe(String message)
    {
        getLogger().severe(message);
    }

    @Override
    public void debug(String message)
    {
        if(debugMode)
        {
            getLogger().info("[Debug] " + message);
        }
    }

    public boolean sendToServer(Player player, String destination)
    {
        return sendToServer(player, destination, true);
    }

    public boolean sendToServer(Player player, String destination, boolean callEvent)
    {
        if(callEvent)
        {
            PlayerServerSwitchEvent event = new PlayerServerSwitchEvent(player, destination);
            Bukkit.getPluginManager().callEvent(event);

            if(event.isCancelled())
            {
                return false;
            }
        }

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Connect");
        output.writeUTF(destination);
        player.sendPluginMessage(this, "BungeeCord", output.toByteArray());
        return true;
    }

    public Set<String> getBlockedServers()
    {
        return Collections.unmodifiableSet(blockedServers);
    }
}

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
import com.yahoo.tracebachi.DeltaEssentials.Commands.JailCommand;
import com.yahoo.tracebachi.DeltaEssentials.Commands.JoinStopCommand;
import com.yahoo.tracebachi.DeltaEssentials.Commands.KickCommand;
import com.yahoo.tracebachi.DeltaEssentials.Commands.MoveToCommand;
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerServerSwitchEvent;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.DeltaTeleport;
import com.yahoo.tracebachi.DeltaRedis.Shared.Interfaces.LoggablePlugin;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/5/15.
 */
public class DeltaEssentialsPlugin extends JavaPlugin implements LoggablePlugin
{
    private boolean debugMode;
    private boolean stopJoin;

    private HashSet<String> blockedServers = new HashSet<>();
    private HashMap<String, HikariDataSource> sources = new HashMap<>();

    private MoveToCommand moveToCommand;
    private KickCommand kickCommand;
    private JailCommand jailCommand;
    private JoinStopCommand joinStopCommand;
    private GeneralListener generalListener;

    private DeltaChat deltaChat;
    private DeltaTeleport deltaTeleport;

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

        PluginManager pluginManager = getServer().getPluginManager();
        DeltaRedisPlugin deltaRedisPlugin = (DeltaRedisPlugin) pluginManager.getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = deltaRedisPlugin.getDeltaRedisApi();

        moveToCommand = new MoveToCommand(deltaRedisApi, this);
        getCommand("moveto").setExecutor(moveToCommand);
        getCommand("moveto").setTabCompleter(moveToCommand);
        kickCommand = new KickCommand(deltaRedisApi);
        getCommand("kick").setExecutor(kickCommand);
        getCommand("kick").setTabCompleter(kickCommand);
        jailCommand = new JailCommand(deltaRedisApi, this);
        getCommand("jail").setExecutor(jailCommand);
        getCommand("jail").setTabCompleter(jailCommand);
        joinStopCommand = new JoinStopCommand(this);
        getCommand("joinstop").setExecutor(joinStopCommand);

        deltaChat = new DeltaChat(deltaRedisApi, this);
        deltaTeleport = new DeltaTeleport(this, deltaRedisApi);

        generalListener = new GeneralListener(this);
        getServer().getPluginManager().registerEvents(generalListener, this);

        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, "BungeeCord");

        createDataSources();
    }

    @Override
    public void onDisable()
    {
        closeDataSources();

        Messenger messenger = getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(this);

        if(generalListener != null)
        {
            generalListener.shutdown();
            generalListener = null;
        }

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

        if(joinStopCommand != null)
        {
            getCommand("joinstop").setExecutor(null);
            joinStopCommand.shutdown();
            joinStopCommand = null;
        }

        if(jailCommand != null)
        {
            getCommand("jail").setExecutor(null);
            getCommand("jail").setTabCompleter(null);
            jailCommand.shutdown();
            jailCommand = null;
        }

        if(kickCommand != null)
        {
            getCommand("kick").setExecutor(null);
            getCommand("kick").setTabCompleter(null);
            kickCommand.shutdown();
            kickCommand = null;
        }

        if(moveToCommand != null)
        {
            getCommand("moveto").setExecutor(null);
            getCommand("moveto").setTabCompleter(null);
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

    public HikariDataSource getDataSource(String name)
    {
        return sources.get(name);
    }

    public void setJoinStop(boolean value)
    {
        stopJoin = value;
    }

    public boolean isJoinStopEnabled()
    {
        return stopJoin;
    }

    private void createDataSources()
    {
        ConfigurationSection section = getConfig().getConfigurationSection("Databases");
        Set<String> sourceNames = section.getKeys(false);

        for(String sourceName : sourceNames)
        {
            String username = section.getString(sourceName + ".Username");
            String password = section.getString(sourceName + ".Password");
            String url = section.getString(sourceName + ".URL");

            try
            {
                getLogger().info("Creating DataSource (" + sourceName + ") ...");
                HikariDataSource dataSource = createDataSource(username, password, url);
                sources.put(sourceName, dataSource);
                getLogger().info("................... Done.");
            }
            catch(Exception ex)
            {
                getLogger().severe("Failed to create DataSource: " + sourceName);
                ex.printStackTrace();
            }
        }
    }

    private void closeDataSources()
    {
        for(Map.Entry<String, HikariDataSource> entry : sources.entrySet())
        {
            try
            {
                // Close the data source
                if(entry.getValue() != null)
                {
                    getLogger().info("Closing DataSource (" + entry.getKey() + ") ...");
                    entry.getValue().close();
                    getLogger().info(".................. Done.");
                }
            }
            catch(Exception ex)
            {
                getLogger().severe("Failed to close DataSource: " + entry.getKey());
                ex.printStackTrace();
            }
        }
    }

    private HikariDataSource createDataSource(String username, String password, String url)
    {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + url);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(config);
    }
}

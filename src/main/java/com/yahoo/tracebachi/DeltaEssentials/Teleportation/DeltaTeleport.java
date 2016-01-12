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
package com.yahoo.tracebachi.DeltaEssentials.Teleportation;

import com.yahoo.tracebachi.DeltaEssentials.DeltaEssentialsPlugin;
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTpEvent;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.Commands.*;
import com.yahoo.tracebachi.DeltaRedis.Shared.Interfaces.LoggablePlugin;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/5/15.
 */
public class DeltaTeleport implements LoggablePlugin
{
    private DeltaEssentialsPlugin plugin;
    private HashMap<String, TpRequest> requestMap;
    private HashSet<String> tpDenySet;
    private BukkitTask cleanupTask;

    private TpCommand tpCommand;
    private TpHereCommand tpHereCommand;
    private TpaHereCommand tpaHereCommand;
    private TpAcceptCommand tpAcceptCommand;
    private TpToggleCommand tpToggleCommand;
    private TpListener tpListener;

    public DeltaTeleport(DeltaEssentialsPlugin plugin, DeltaRedisApi deltaRedisApi)
    {
        this.plugin = plugin;
        this.requestMap = new HashMap<>();
        this.tpDenySet = new HashSet<>();

        this.tpCommand = new TpCommand(deltaRedisApi, this);
        this.tpHereCommand = new TpHereCommand(deltaRedisApi, this);
        this.tpaHereCommand = new TpaHereCommand(deltaRedisApi, this);
        this.tpAcceptCommand = new TpAcceptCommand(deltaRedisApi, this);
        this.tpToggleCommand = new TpToggleCommand(this);

        plugin.getCommand("tp").setExecutor(tpCommand);
        plugin.getCommand("tp").setTabCompleter(tpCommand);
        plugin.getCommand("tphere").setExecutor(tpHereCommand);
        plugin.getCommand("tphere").setTabCompleter(tpHereCommand);
        plugin.getCommand("tpahere").setExecutor(tpaHereCommand);
        plugin.getCommand("tpahere").setTabCompleter(tpaHereCommand);
        plugin.getCommand("tpaccept").setExecutor(tpAcceptCommand);
        plugin.getCommand("tptoggle").setExecutor(tpToggleCommand);

        this.tpListener = new TpListener(deltaRedisApi, this);
        plugin.getServer().getPluginManager().registerEvents(tpListener, plugin);

        this.cleanupTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () ->
        {
            Iterator<Map.Entry<String, TpRequest>> iterator = requestMap.entrySet().iterator();
            long oldestTime = System.currentTimeMillis() - 60000;

            while(iterator.hasNext())
            {
                TpRequest request = iterator.next().getValue();
                if(request.getTimeCreatedAt() < oldestTime)
                {
                    iterator.remove();
                }
            }
        }, 600, 600);
    }

    public void shutdown()
    {
        if(cleanupTask != null)
        {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        if(tpListener != null)
        {
            tpListener.shutdown();
            tpListener = null;
        }

        if(tpAcceptCommand != null)
        {
            plugin.getCommand("tpaccept").setExecutor(null);
            tpAcceptCommand.shutdown();
            tpAcceptCommand = null;
        }

        if(tpaHereCommand != null)
        {
            plugin.getCommand("tpahere").setExecutor(null);
            plugin.getCommand("tpahere").setTabCompleter(null);
            tpaHereCommand.shutdown();
            tpaHereCommand = null;
        }

        if(tpHereCommand != null)
        {
            plugin.getCommand("tphere").setExecutor(null);
            plugin.getCommand("tphere").setTabCompleter(null);
            tpHereCommand.shutdown();
            tpHereCommand = null;
        }

        if(tpCommand != null)
        {
            plugin.getCommand("tp").setExecutor(null);
            plugin.getCommand("tp").setTabCompleter(null);
            tpCommand.shutdown();
            tpCommand = null;
        }

        if(tpDenySet != null)
        {
            tpDenySet.clear();
            tpDenySet = null;
        }

        if(requestMap != null)
        {
            requestMap.clear();
            requestMap = null;
        }

        this.plugin = null;
    }

    public void addTpRequest(String name, String destName)
    {
        requestMap.put(name.toLowerCase(), new TpRequest(destName, null));
    }

    public void addTpRequest(String name, TpRequest request)
    {
        requestMap.put(name.toLowerCase(), request);
    }

    public TpRequest getTpRequest(String name)
    {
        return requestMap.get(name.toLowerCase());
    }

    public TpRequest removeTpRequest(String name)
    {
        return requestMap.remove(name.toLowerCase());
    }

    public boolean isDenyingTp(String name)
    {
        return tpDenySet.contains(name.toLowerCase());
    }

    public void addTpDeny(String name)
    {
        tpDenySet.add(name.toLowerCase());
    }

    public boolean removeTpDeny(String name)
    {
        return tpDenySet.remove(name.toLowerCase());
    }

    public PlayerTpEvent teleportWithEvent(Player playerToTp, Player destination)
    {
        PlayerTpEvent event = new PlayerTpEvent(playerToTp, destination);
        Bukkit.getPluginManager().callEvent(event);

        if(!event.isCancelled())
        {
            playerToTp.sendMessage(Prefixes.SUCCESS + "Teleporting ...");
            playerToTp.teleport(destination, PlayerTeleportEvent.TeleportCause.COMMAND);
        }
        return event;
    }

    public boolean sendToServer(Player player, String destination)
    {
        return plugin.sendToServer(player, destination, true);
    }

    @Override
    public void info(String message)
    {
        plugin.getLogger().info(message);
    }

    @Override
    public void severe(String message)
    {
        plugin.getLogger().severe(message);
    }

    @Override
    public void debug(String message)
    {
        plugin.debug(message);
    }
}

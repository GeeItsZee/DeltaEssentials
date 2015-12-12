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
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTeleportEvent;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.Commands.TpAcceptCommand;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.Commands.TpCommand;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.Commands.TpHereCommand;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.Commands.TpaCommand;
import com.yahoo.tracebachi.DeltaRedis.Shared.Interfaces.LoggablePlugin;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/5/15.
 */
public class DeltaTeleport implements LoggablePlugin
{
    private DeltaEssentialsPlugin plugin;
    private HashMap<String, TpRequest> requestMap;
    private BukkitTask cleanupTask;

    private TpCommand tpCommand;
    private TpHereCommand tpHereCommand;
    private TpaCommand tpaCommand;
    private TpAcceptCommand tpAcceptCommand;

    private TpListener tpListener;

    public DeltaTeleport(DeltaEssentialsPlugin plugin, DeltaRedisApi deltaRedisApi)
    {
        this.plugin = plugin;
        this.requestMap = new HashMap<>();

        tpCommand = new TpCommand(this, deltaRedisApi);
        tpHereCommand = new TpHereCommand(this, deltaRedisApi);
        tpaCommand = new TpaCommand(this, deltaRedisApi);
        tpAcceptCommand = new TpAcceptCommand(this);

        plugin.getCommand("tp").setExecutor(tpCommand);
        plugin.getCommand("tphere").setExecutor(tpHereCommand);
        plugin.getCommand("tpahere").setExecutor(tpaCommand);
        plugin.getCommand("tpaccept").setExecutor(tpAcceptCommand);

        tpListener = new TpListener(this);
        plugin.getServer().getPluginManager().registerEvents(tpListener, plugin);

        cleanupTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () ->
        {
            Iterator<Map.Entry<String, TpRequest>> iterator = requestMap.entrySet().iterator();
            long currentTime = System.currentTimeMillis();

            while(iterator.hasNext())
            {
                TpRequest request = iterator.next().getValue();
                if((currentTime - request.getTimeCreatedAt()) > 60000)
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

        if(tpaCommand != null)
        {
            plugin.getCommand("tpahere").setExecutor(null);
            tpaCommand.shutdown();
            tpaCommand = null;
        }

        if(tpHereCommand != null)
        {
            plugin.getCommand("tphere").setExecutor(null);
            tpHereCommand.shutdown();
            tpHereCommand = null;
        }

        if(tpCommand != null)
        {
            plugin.getCommand("tp").setExecutor(null);
            tpCommand.shutdown();
            tpCommand = null;
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

    public PlayerTeleportEvent teleportWithEvent(Player playerToTp, Player destination)
    {
        PlayerTeleportEvent event = new PlayerTeleportEvent(playerToTp, destination);
        Bukkit.getPluginManager().callEvent(event);

        if(!event.isCancelled())
        {
            playerToTp.teleport(destination);
        }
        return event;
    }

    public boolean sendToServer(Player player, String destination)
    {
        return plugin.sendToServer(player, destination, true);
    }

    public boolean sendToServer(Player player, String destination, boolean callEvent)
    {
        return plugin.sendToServer(player, destination, callEvent);
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

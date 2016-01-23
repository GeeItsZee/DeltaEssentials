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

import com.earth2me.essentials.utils.LocationUtil;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsChannels;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTpEvent;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssentialsPlayer;
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class TeleportListener extends DeltaEssentialsListener
{
    private DeltaRedisApi deltaRedisApi;
    private BukkitTask requestCleanupTask;
    private CaseInsensitiveHashMap<TeleportRequest> requestMap;

    public TeleportListener(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super(plugin);
        this.requestMap = new CaseInsensitiveHashMap<>();
        this.deltaRedisApi = deltaRedisApi;
        this.requestCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
        {
            long oldestTime = System.currentTimeMillis() - 60000;
            Iterator<Map.Entry<String, TeleportRequest>> iterator =
                requestMap.entrySet().iterator();

            while(iterator.hasNext())
            {
                TeleportRequest request = iterator.next().getValue();
                if(request.getTimeCreatedAt() < oldestTime)
                {
                    iterator.remove();
                }
            }
        }, 600, 600);
    }

    @Override
    public void shutdown()
    {
        this.requestMap.clear();
        this.requestMap = null;
        this.requestCleanupTask.cancel();
        this.requestCleanupTask = null;
        this.deltaRedisApi = null;
        super.shutdown();
    }

    public CaseInsensitiveHashMap<TeleportRequest> getRequestMap()
    {
        return requestMap;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        requestMap.remove(player.getName());
    }

    @EventHandler
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();
        String message = event.getMessage();

        if(channel.equals(DeltaEssentialsChannels.TP))
        {
            // Format: TpSender/\DestName
            String[] split = DELTA_PATTERN.split(message, 2);
            handleTpChannel(split[0], split[1]);
        }
        else if(channel.equals(DeltaEssentialsChannels.TP_HERE))
        {
            // Format: Receiver/\Sender/\DestServer
            String[] split = DELTA_PATTERN.split(message, 3);
            handleTpHereChannel(split[0], split[1], split[2]);
        }
        else if(channel.equals(DeltaEssentialsChannels.TPA_HERE))
        {
            // Format: Receiver/\Sender/\DestServer
            String[] split = DELTA_PATTERN.split(message, 3);
            handleTpaHereChannel(split[0], split[1], split[2]);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player toTp = event.getPlayer();
        String playerName = toTp.getName();

        TeleportRequest request = requestMap.remove(playerName);
        if(request == null) { return; }

        String destName = request.getSender();
        Player destination = Bukkit.getPlayer(destName);

        if(destination == null || !destination.isOnline())
        {
            toTp.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not online.");
        }
        else
        {
            teleport(toTp, destination);
        }
    }

    public void teleport(Player toTp, Player destination)
    {
        String senderName = toTp.getName();
        String destName = destination.getName();
        Location location = destination.getLocation();
        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(destName);

        if(!toTp.canSee(destination) && !toTp.hasPermission("DeltaEss.TpVanishBypass"))
        {
            toTp.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not online.");
            return;
        }

        if(dePlayer != null && dePlayer.isTeleportDenyEnabled() &&
            !toTp.hasPermission("DeltaEss.TpDenyBypass"))
        {
            toTp.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not allowing players to teleport to them.");
            destination.sendMessage(Prefixes.INFO + Prefixes.input(senderName) +
                " tried to teleport to you, but you are denying teleports.");
            return;
        }

        PlayerTpEvent event = new PlayerTpEvent(toTp, destination);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()) { return; }

        try
        {
            if(!toTp.getAllowFlight() || !toTp.isFlying())
            {
                location = LocationUtil.getSafeDestination(location);
            }

            toTp.sendMessage(Prefixes.SUCCESS + "Teleporting ...");
            toTp.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND);

            if(!toTp.hasPermission("DeltaEss.SilentTp"))
            {
                destination.sendMessage(Prefixes.INFO + Prefixes.input(senderName) +
                    " teleported to you.");
            }
        }
        catch(Exception ex)
        {
            toTp.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is in an unsafe teleport location.");
        }
    }

    private void handleTpChannel(String tpSender, String destName)
    {
        TeleportRequest request = new TeleportRequest(destName, deltaRedisApi.getServerName());
        Player toTp = Bukkit.getPlayer(tpSender);

        if(toTp == null || !toTp.isOnline())
        {
            requestMap.put(tpSender, request);
            return;
        }

        Player destination = Bukkit.getPlayer(destName);

        if(destination == null || !destination.isOnline())
        {
            toTp.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not online.");
        }
        else
        {
            teleport(toTp, destination);
        }
    }

    private void handleTpHereChannel(String nameToTp, String tpHereSender, String destServer)
    {
        Player player = Bukkit.getPlayer(nameToTp);
        if(player != null && player.isOnline())
        {
            plugin.sendToServer(player, destServer);
        }
        else
        {
            deltaRedisApi.sendMessageToPlayer(tpHereSender, Prefixes.FAILURE +
                Prefixes.input(nameToTp) + " is not online.");
        }
    }

    private void handleTpaHereChannel(String receiver, String sender, String destServer)
    {
        TeleportRequest request = new TeleportRequest(sender, destServer);
        Player receiverPlayer = Bukkit.getPlayer(receiver);

        if(receiverPlayer != null && receiverPlayer.isOnline())
        {
            receiverPlayer.sendMessage(Prefixes.INFO + Prefixes.input(sender) +
                " wants you to TP to them. Use /tpaccept to accept within 30 seconds.");
            requestMap.put(receiver, request);
        }
        else
        {
            deltaRedisApi.sendMessageToPlayer(sender, Prefixes.FAILURE +
                Prefixes.input(receiver) + " is not online.");
        }
    }
}

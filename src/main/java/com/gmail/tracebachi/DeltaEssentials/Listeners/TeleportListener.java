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
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssentialsPlayer;
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
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
        this.requestCleanupTask = Bukkit.getScheduler()
            .runTaskTimer(plugin, this::cleanRequestMap, 100, 100);
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
        Settings settings = plugin.getSettings();
        Player toTp = event.getPlayer();
        String playerName = toTp.getName();

        TeleportRequest request = requestMap.remove(playerName);
        if(request == null) { return; }

        String destName = request.getSender();
        Player destination = Bukkit.getPlayer(destName);

        if(destination == null)
        {
            String playerNotOnline = settings.format("PlayerNotOnline", destName);
            toTp.sendMessage(playerNotOnline);
        }
        else
        {
            teleport(toTp, destination);
        }
    }

    public void teleport(Player toTp, Player destination)
    {
        Settings settings = plugin.getSettings();
        String senderName = toTp.getName();
        String destName = destination.getName();
        Location location = destination.getLocation();
        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(destName);

        if(!toTp.canSee(destination) && !toTp.hasPermission("DeltaEss.TpVanishBypass"))
        {
            String playerNotOnline = settings.format("PlayerNotOnline", destName);
            toTp.sendMessage(playerNotOnline);
            return;
        }

        if(dePlayer != null && dePlayer.isTeleportDenyEnabled() &&
            !toTp.hasPermission("DeltaEss.TpDenyBypass"))
        {
            String deniedTpSender = settings.format("DeniedTpSender", destName);
            String deniedTpReceiver = settings.format("DeniedTpReceiver", senderName);

            toTp.sendMessage(deniedTpSender);
            destination.sendMessage(deniedTpReceiver);
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

            String tpAttempt = settings.format("TpAttempt", destName);
            toTp.sendMessage(tpAttempt);

            toTp.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND);

            if(!toTp.hasPermission("DeltaEss.SilentTp"))
            {
                String tpAlert = settings.format("TpAlert", destName);
                destination.sendMessage(tpAlert);
            }
        }
        catch(Exception ex)
        {
            String unsafeTpLocation = settings.format("UnsafeTpLocation", destName);
            toTp.sendMessage(unsafeTpLocation);
        }
    }

    private void handleTpChannel(String tpSender, String destName)
    {
        Settings settings = plugin.getSettings();
        TeleportRequest request = new TeleportRequest(destName, deltaRedisApi.getServerName());
        Player toTp = Bukkit.getPlayer(tpSender);

        if(toTp == null)
        {
            requestMap.put(tpSender, request);
            return;
        }

        Player destination = Bukkit.getPlayer(destName);

        if(destination == null)
        {
            String playerNotOnline = settings.format("PlayerNotOnline", destName);
            toTp.sendMessage(playerNotOnline);
        }
        else
        {
            teleport(toTp, destination);
        }
    }

    private void handleTpHereChannel(String nameToTp, String tpHereSender, String destServer)
    {
        Settings settings = plugin.getSettings();
        Player player = Bukkit.getPlayer(nameToTp);

        if(player != null)
        {
            plugin.sendToServer(player, destServer);
        }
        else
        {
            String playerNotOnline = settings.format("PlayerNotOnline", nameToTp);
            deltaRedisApi.sendMessageToPlayer(tpHereSender, playerNotOnline);
        }
    }

    private void handleTpaHereChannel(String receiver, String sender, String destServer)
    {
        Settings settings = plugin.getSettings();
        TeleportRequest request = new TeleportRequest(sender, destServer);
        Player receiverPlayer = Bukkit.getPlayer(receiver);

        if(receiverPlayer != null)
        {
            String tpaReceived = settings.format("TpaReceived", sender);
            receiverPlayer.sendMessage(tpaReceived);

            requestMap.put(receiver, request);
        }
        else
        {
            String playerNotOnline = settings.format("PlayerNotOnline", receiver);
            deltaRedisApi.sendMessageToPlayer(sender, playerNotOnline);
        }
    }

    private void cleanRequestMap()
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
    }
}

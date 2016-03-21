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
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerLoadedEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTpEvent;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;

import static com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent.DELTA_PATTERN;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class TeleportListener extends DeltaEssentialsListener
{
    private DeltaRedisApi deltaRedisApi;
    private BukkitTask requestCleanupTask;
    private CaseInsensitiveHashMap<TeleportRequest> requestMap = new CaseInsensitiveHashMap<>();

    public TeleportListener(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super(plugin);
        this.deltaRedisApi = deltaRedisApi;
        this.requestCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 20, 20);
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        requestMap.remove(player.getName());
    }

    @EventHandler(priority = EventPriority.NORMAL)
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLoad(PlayerLoadedEvent event)
    {
        Player toTp = event.getPlayer();
        String playerName = toTp.getName();

        TeleportRequest request = requestMap.remove(playerName);

        if(request == null) return;

        String destName = request.getSender();
        Player destination = Bukkit.getPlayer(destName);

        if(destination == null)
        {
            toTp.sendMessage(Settings.format("PlayerOffline", destName));
            return;
        }

        teleport(toTp, destination, request.getTeleportType());
    }

    public boolean teleport(Player toTp, Player destination, PlayerTpEvent.TeleportType teleportType)
    {
        String senderName = toTp.getName();
        String destName = destination.getName();
        Location location = destination.getLocation();
        DeltaEssPlayerData playerData = plugin.getPlayerMap().get(destName);

        if(teleportType == PlayerTpEvent.TeleportType.NORMAL_TP &&
            (!toTp.canSee(destination) || playerData.isVanishEnabled()) &&
            !toTp.hasPermission("DeltaEss.TpVanishBypass"))
        {
            toTp.sendMessage(Settings.format("PlayerOffline", destName));
            return false;
        }

        if(playerData != null && playerData.isTeleportDenyEnabled() &&
            !toTp.hasPermission("DeltaEss.TpDenyBypass"))
        {
            toTp.sendMessage(Settings.format("OtherDenyingTeleport", destName));
            destination.sendMessage(Settings.format("DeniedTeleport", senderName));
            return false;
        }

        try
        {
            if(!toTp.getAllowFlight() || !toTp.isFlying())
            {
                location = LocationUtil.getSafeDestination(location);
            }
        }
        catch(Exception ex)
        {
            toTp.sendMessage(Settings.format("UnsafeLocation", destName));
            return false;
        }

        PlayerTpEvent event = new PlayerTpEvent(toTp, destination, teleportType);
        Bukkit.getPluginManager().callEvent(event);

        if(!event.isCancelled())
        {
            toTp.sendMessage(Settings.format("TeleportingMessage", destName));

            toTp.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND);

            if(!toTp.hasPermission("DeltaEss.SilentTp"))
            {
                destination.sendMessage(Settings.format("TeleportedToYou", senderName));
            }

            return true;
        }
        return false;
    }

    private void handleTpChannel(String tpSender, String destName)
    {
        String destServer = deltaRedisApi.getServerName();
        TeleportRequest request = new TeleportRequest(destName, destServer,
            PlayerTpEvent.TeleportType.NORMAL_TP);
        Player toTp = Bukkit.getPlayer(tpSender);

        if(toTp == null)
        {
            requestMap.put(tpSender, request);
            return;
        }

        Player destination = Bukkit.getPlayer(destName);

        if(destination == null)
        {
            toTp.sendMessage(Settings.format("PlayerOffline", destName));
            return;
        }

        teleport(toTp, destination, PlayerTpEvent.TeleportType.NORMAL_TP);
    }

    private void handleTpHereChannel(String nameToTp, String tpHereSender, String destServer)
    {
        Player player = Bukkit.getPlayer(nameToTp);

        if(player == null)
        {
            deltaRedisApi.sendMessageToPlayer(tpHereSender,
                Settings.format("PlayerOffline", nameToTp));
            return;
        }

        plugin.sendToServer(player, destServer);
    }

    private void handleTpaHereChannel(String receiver, String sender, String destServer)
    {
        TeleportRequest request = new TeleportRequest(sender, destServer,
            PlayerTpEvent.TeleportType.TPA_HERE);
        Player receiverPlayer = Bukkit.getPlayer(receiver);

        if(receiverPlayer == null)
        {
            deltaRedisApi.sendMessageToPlayer(sender,
                Settings.format("PlayerOffline", receiver));
            return;
        }

        DeltaEssPlayerData playerData = plugin.getPlayerMap().get(receiver);

        if(playerData != null && playerData.isVanishEnabled())
        {
            deltaRedisApi.sendMessageToPlayer(sender,
                Settings.format("PlayerOffline", receiver));
            return;
        }

        receiverPlayer.sendMessage(Settings.format("ReceivedTeleportRequest", sender));
        requestMap.put(receiver, request);
    }

    private void cleanup()
    {
        long oldestTime = System.currentTimeMillis() - 30000;
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

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
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsChannels;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerPostLoadEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTpEvent;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
import com.gmail.tracebachi.DeltaRedis.Shared.SplitPatterns;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
    private BukkitTask requestCleanupTask;
    private CaseInsensitiveHashMap<TeleportRequest> requestMap = new CaseInsensitiveHashMap<>();

    public TeleportListener(DeltaEssentials plugin)
    {
        super(plugin);

        this.requestCleanupTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            this::cleanup,
            20,
            20);
    }

    @Override
    public void shutdown()
    {
        this.requestMap.clear();
        this.requestMap = null;
        this.requestCleanupTask.cancel();
        this.requestCleanupTask = null;
        super.shutdown();
    }

    public CaseInsensitiveHashMap<TeleportRequest> getRequestMap()
    {
        return requestMap;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();
        String message = event.getMessage();

        if(channel.equals(DeltaEssentialsChannels.TP))
        {
            // Format: Sender/\DestName
            String[] split = SplitPatterns.DELTA.split(message, 2);
            handleTpChannel(split[0], split[1]);
        }
        else if(channel.equals(DeltaEssentialsChannels.TP_HERE))
        {
            // Format: Receiver/\Sender/\DestServer
            String[] split = SplitPatterns.DELTA.split(message, 3);
            handleTpHereChannel(split[0], split[1], split[2]);
        }
        else if(channel.equals(DeltaEssentialsChannels.TPA_HERE))
        {
            // Format: Receiver/\Sender/\DestServer
            String[] split = SplitPatterns.DELTA.split(message, 3);
            handleTpaHereChannel(split[0], split[1], split[2]);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLoad(PlayerPostLoadEvent event)
    {
        Player toTp = event.getPlayer();
        String playerName = toTp.getName();

        TeleportRequest request = requestMap.remove(playerName);

        if(request == null) { return; }

        String destName = request.getSender();
        Player destination = Bukkit.getPlayerExact(destName);

        if(destination == null)
        {
            toTp.sendMessage(Settings.format("PlayerOffline", destName));
            return;
        }

        teleport(toTp, destination, request.getTeleportType());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        requestMap.remove(player.getName());
    }

    public boolean teleport(Player toTp, Player destination,
                            PlayerTpEvent.TeleportType teleportType)
    {
        String senderName = toTp.getName();
        String destName = destination.getName();
        DeltaEssPlayerData destPlayerData = plugin.getPlayerMap().get(destName);

        if(teleportType == PlayerTpEvent.TeleportType.NORMAL_TP)
        {
            if((destPlayerData == null || destPlayerData.isVanished()) &&
                !toTp.hasPermission("DeltaEss.Tp.IgnoreVanish"))
            {
                toTp.sendMessage(Settings.format("PlayerOffline", destName));
                return false;
            }

            if((destPlayerData == null || destPlayerData.isDenyingTeleports()) &&
                !toTp.hasPermission("DeltaEss.Tp.IgnoreDeny"))
            {
                toTp.sendMessage(Settings.format("OtherDenyingTeleport", destName));
                destination.sendMessage(Settings.format("DeniedTeleport", senderName));
                return false;
            }
        }

        Location location = getSafeDestination(destination.getLocation());

        if(location == null)
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

            if(!toTp.hasPermission("DeltaEss.Tp.Silent"))
            {
                destination.sendMessage(Settings.format("TeleportedToYou", senderName));
            }

            return true;
        }

        return false;
    }

    private void handleTpChannel(String tpSender, String destName)
    {
        String destServer = DeltaRedisApi.instance().getServerName();
        Player toTp = Bukkit.getPlayerExact(tpSender);
        TeleportRequest request = new TeleportRequest(
            destName,
            destServer,
            PlayerTpEvent.TeleportType.NORMAL_TP);

        if(toTp == null)
        {
            requestMap.put(tpSender, request);
            return;
        }

        Player destination = Bukkit.getPlayerExact(destName);

        if(destination == null)
        {
            toTp.sendMessage(Settings.format("PlayerOffline", destName));
            return;
        }

        teleport(toTp, destination, PlayerTpEvent.TeleportType.NORMAL_TP);
    }

    private void handleTpHereChannel(String nameToTp, String tpHereSender, String destServer)
    {
        Player player = Bukkit.getPlayerExact(nameToTp);

        if(player == null)
        {
            DeltaRedisApi.instance().sendMessageToPlayer(
                tpHereSender,
                Settings.format("PlayerOffline", nameToTp));
            return;
        }

        plugin.sendToServer(player, destServer);
    }

    private void handleTpaHereChannel(String receiver, String sender, String destServer)
    {
        DeltaRedisApi deltaRedisApi = DeltaRedisApi.instance();
        Player receiverPlayer = Bukkit.getPlayerExact(receiver);
        TeleportRequest request = new TeleportRequest(
            sender,
            destServer,
            PlayerTpEvent.TeleportType.TPA_HERE);

        if(receiverPlayer == null)
        {
            deltaRedisApi.sendMessageToPlayer(
                sender,
                Settings.format("PlayerOffline", receiver));
            return;
        }

        requestMap.put(receiver, request);
        receiverPlayer.sendMessage(Settings.format("ReceivedTeleportRequest", sender));
    }

    private Location getSafeDestination(Location start)
    {
        if(start == null || start.getWorld() == null) { return null; }

        World world = start.getWorld();
        int startX = start.getBlockX();
        int startY = start.getBlockY();
        int startZ = start.getBlockZ();
        int nonAirBlockY = startY;

        while(nonAirBlockY > 0)
        {
            Block block = world.getBlockAt(startX, nonAirBlockY, startZ);

            if(!block.isEmpty())
            {
                switch(block.getType())
                {
                    case LAVA:
                    case STATIONARY_LAVA:
                    case FIRE:
                        return null;

                    default:
                        return block.getLocation().add(0.5, 1.1, 0.5);
                }
            }

            nonAirBlockY--;
        }

        return null;
    }

    private void cleanup()
    {
        long oldestTime = System.currentTimeMillis() - 30000;
        Iterator<Map.Entry<String, TeleportRequest>> iterator = requestMap.entrySet().iterator();

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

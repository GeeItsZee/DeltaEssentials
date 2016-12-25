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
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.Events.DeltaRedisMessageEvent;
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
import java.util.List;
import java.util.Map;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;
import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.formatPlayerOffline;

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
        List<String> messageParts = event.getMessageParts();

        if(channel.equals(DeltaEssentialsChannels.TP))
        {
            String commandRunnerName = messageParts.get(0);
            String destName = messageParts.get(1);
            handleTpChannel(commandRunnerName, destName);
        }
        else if(channel.equals(DeltaEssentialsChannels.TP_HERE))
        {
            String commandRunnerName = messageParts.get(0);
            String toTpHereName = messageParts.get(1);
            String destServer = messageParts.get(2);
            handleTpHereChannel(commandRunnerName, toTpHereName, destServer);
        }
        else if(channel.equals(DeltaEssentialsChannels.TPA_HERE))
        {
            String commandRunnerName = messageParts.get(0);
            String toTpaHereName = messageParts.get(1);
            String destServer = messageParts.get(2);
            handleTpaHereChannel(commandRunnerName, toTpaHereName, destServer);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLoad(PlayerPostLoadEvent event)
    {
        Player toTp = event.getPlayer();
        String playerName = toTp.getName();
        TeleportRequest request = requestMap.remove(playerName);

        if(request == null) { return; }

        String senderName = request.getSender();
        Player sender = Bukkit.getPlayerExact(senderName);

        if(sender == null)
        {
            toTp.sendMessage(formatPlayerOffline(senderName));
            return;
        }

        teleport(toTp, sender, request.getTeleportType());
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

        if(teleportType == PlayerTpEvent.TeleportType.NORMAL_TP)
        {
            DeltaEssPlayerData destPlayerData = plugin.getPlayerDataMap().get(destName);

            if((destPlayerData != null && destPlayerData.isVanished()) &&
                !toTp.hasPermission("DeltaEss.Tp.IgnoreVanish"))
            {
                toTp.sendMessage(formatPlayerOffline(destName));
                return false;
            }

            if((destPlayerData != null && destPlayerData.isDenyingTeleports()) &&
                !toTp.hasPermission("DeltaEss.Tp.IgnoreDeny"))
            {
                toTp.sendMessage(format("DeltaEss.OtherDenyingTeleport", destName));
                destination.sendMessage(format("DeltaEss.DeniedTeleport", senderName));
                return false;
            }
        }

        Location location = getSafeDestination(destination.getLocation());
        if(location == null)
        {
            toTp.sendMessage(format("DeltaEss.UnsafeLocation", destName));
            return false;
        }

        PlayerTpEvent event = new PlayerTpEvent(toTp, destination, teleportType);
        Bukkit.getPluginManager().callEvent(event);

        if(event.isCancelled()) { return false; }

        toTp.sendMessage(format("DeltaEss.TeleportingMessage", destName));
        toTp.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND);

        if(!toTp.hasPermission("DeltaEss.Tp.Silent"))
        {
            destination.sendMessage(format("DeltaEss.TeleportedToYou", senderName));
        }

        return true;
    }

    private void handleTpChannel(String commandRunnerName, String destName)
    {
        String destServer = DeltaRedisApi.instance().getServerName();
        Player commandRunner = Bukkit.getPlayerExact(commandRunnerName);

        if(commandRunner == null)
        {
            TeleportRequest request = new TeleportRequest(
                destName,
                destServer,
                PlayerTpEvent.TeleportType.NORMAL_TP);

            requestMap.put(commandRunnerName, request);
            return;
        }

        Player destination = Bukkit.getPlayerExact(destName);
        if(destination == null)
        {
            commandRunner.sendMessage(formatPlayerOffline(destName));
        }
        else
        {
            teleport(commandRunner, destination, PlayerTpEvent.TeleportType.NORMAL_TP);
        }
    }

    private void handleTpHereChannel(String commandRunnerName, String toTpHereName, String destServer)
    {
        Player toTpHere = Bukkit.getPlayerExact(toTpHereName);
        if(toTpHere == null)
        {
            DeltaRedisApi.instance().sendMessageToPlayer(
                commandRunnerName,
                formatPlayerOffline(toTpHereName));
        }
        else
        {
            plugin.sendToServer(toTpHere, destServer);
        }
    }

    private void handleTpaHereChannel(String commandRunnerName, String toTpaHereName, String destServer)
    {
        DeltaRedisApi deltaRedisApi = DeltaRedisApi.instance();
        Player toTpaHere = Bukkit.getPlayerExact(toTpaHereName);

        if(toTpaHere == null)
        {
            deltaRedisApi.sendMessageToPlayer(
                commandRunnerName,
                formatPlayerOffline(toTpaHereName));
        }
        else
        {
            TeleportRequest request = new TeleportRequest(
                commandRunnerName,
                destServer,
                PlayerTpEvent.TeleportType.TPA_HERE);

            requestMap.put(toTpaHereName, request);
            toTpaHere.sendMessage(format("DeltaEss.ReceivedTeleportRequest", commandRunnerName));
        }
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

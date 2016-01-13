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

import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTpEvent;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class TpListener implements Listener
{
    public static final String TP_CHANNEL = "DE-TP";
    public static final String TPHERE_CHANNEL = "DE-TPHere";
    public static final String TPAHERE_CHANNEL = "DE-TPA";
    private static final Pattern pattern = Pattern.compile("/\\\\");

    private DeltaRedisApi deltaRedisApi;
    private DeltaTeleport deltaTeleport;

    public TpListener(DeltaRedisApi deltaRedisApi, DeltaTeleport deltaTeleport)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.deltaTeleport = deltaTeleport;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
        this.deltaTeleport = null;
    }

    @EventHandler
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(TP_CHANNEL))
        {
            // Format: TpSender/\DestName
            String[] splitMessage = pattern.split(event.getMessage(), 2);
            handleTpChannel(splitMessage[0], splitMessage[1]);
        }
        else if(event.getChannel().equals(TPHERE_CHANNEL))
        {
            // Format: NameToTp/\TpHereSender/\DestServer
            String[] splitMessage = pattern.split(event.getMessage(), 3);
            handleTpHereChannel(splitMessage[0], splitMessage[1], splitMessage[2]);
        }
        else if(event.getChannel().equals(TPAHERE_CHANNEL))
        {
            // Format: TpaReceiver/\TpaSender/\DestServer
            String[] splitMessage = pattern.split(event.getMessage(), 3);
            handleTpaHereChannel(splitMessage[0], splitMessage[1], splitMessage[2]);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        String playerName = player.getName();
        TpRequest request = deltaTeleport.removeTpRequest(playerName);

        // Ignore if there is no request
        if(request == null) { return; }

        String destName = request.getSender();
        Player destination = Bukkit.getPlayer(destName);
        if(destination == null || !destination.isOnline())
        {
            player.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not online.");
            return;
        }

        if(!player.canSee(destination) && !player.hasPermission("DeltaEss.Tp.IgnoreVanish"))
        {
            player.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not online.");
            return;
        }

        boolean denyingTp = deltaTeleport.isDenyingTp(destName);
        if(denyingTp && !player.hasPermission("DeltaEss.TpToggle.Bypass"))
        {
            player.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not allowing players to teleport to them.");
            destination.sendMessage(Prefixes.INFO + Prefixes.input(playerName) +
                " tried to teleport to you, but you are denying teleports.");
        }
        else
        {
            deltaTeleport.teleportWithEvent(player, destination);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        deltaTeleport.removeTpDeny(player.getName());
        deltaTeleport.removeTpRequest(player.getName());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTpEvent event)
    {
        String playerName = event.getPlayerToTeleport().getName();
        Player dest = event.getDestination();

        if(dest.hasPermission("DeltaEss.Tp.Alert"))
        {
            dest.sendMessage(Prefixes.INFO + Prefixes.input(playerName) +
                " teleported to you.");
        }
    }

    private void handleTpChannel(String tpSender, String destName)
    {
        Player player = Bukkit.getPlayer(tpSender);
        if(player == null || !player.isOnline())
        {
            deltaTeleport.addTpRequest(tpSender, destName);
            return;
        }

        Player destination = Bukkit.getPlayer(destName);
        if(destination == null || !destination.isOnline())
        {
            player.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not online.");
            return;
        }

        if(!player.canSee(destination) && !player.hasPermission("DeltaEss.Tp.IgnoreVanish"))
        {
            player.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not online.");
            return;
        }

        boolean denyingTp = deltaTeleport.isDenyingTp(destName);
        if(denyingTp && !player.hasPermission("DeltaEss.TpToggle.Bypass"))
        {
            player.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not allowing players to teleport to them.");
            destination.sendMessage(Prefixes.INFO + Prefixes.input(tpSender) +
                " tried to teleport to you, but you are denying teleports.");
        }
        else
        {
            deltaTeleport.teleportWithEvent(player, destination);
        }
    }

    private void handleTpHereChannel(String nameToTp, String tpHereSender, String destServer)
    {
        Player player = Bukkit.getPlayer(nameToTp);
        if(player != null && player.isOnline())
        {
            deltaTeleport.sendToServer(player, destServer);
        }
        else
        {
            deltaRedisApi.sendMessageToPlayer(tpHereSender, Prefixes.FAILURE +
                Prefixes.input(nameToTp) + " is not online.");
        }
    }

    private void handleTpaHereChannel(String tpaReceiver, String tpaSender, String destServer)
    {
        Player targetPlayer = Bukkit.getPlayer(tpaReceiver);
        if(targetPlayer != null && targetPlayer.isOnline())
        {
            targetPlayer.sendMessage(Prefixes.INFO + Prefixes.input(tpaSender) +
                " wants you to TP to them. Use /tpaccept to accept within 30 seconds.");
            deltaTeleport.addTpRequest(tpaReceiver,
                new TpRequest(tpaSender, destServer));
        }
        else
        {
            deltaRedisApi.sendMessageToPlayer(tpaSender, Prefixes.FAILURE +
                Prefixes.input(tpaReceiver) + " is not online.");
        }
    }
}

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

import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTeleportEvent;
import com.yahoo.tracebachi.DeltaEssentials.Prefixes;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class TpListener implements Listener
{
    private static final Pattern pattern = Pattern.compile("/\\\\");

    private DeltaTeleport deltaTeleport;

    public TpListener(DeltaTeleport deltaTeleport)
    {
        this.deltaTeleport = deltaTeleport;
    }

    public void shutdown()
    {
        this.deltaTeleport = null;
    }

    @EventHandler
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals("DeltaEss:Tp"))
        {
            // Format: SenderName/\DestName
            String[] splitMessage = pattern.split(event.getMessage(), 2);
            String senderName = splitMessage[0];
            String destName = splitMessage[1];
            Player destPlayer = Bukkit.getPlayer(destName);

            if(destPlayer != null && destPlayer.isOnline())
            {
                Player player = Bukkit.getPlayer(senderName);

                if(player != null && player.isOnline())
                {
                    deltaTeleport.teleportWithEvent(player, destPlayer);
                }
                else
                {
                    deltaTeleport.addTpRequest(senderName, destName);
                }
            }
        }
        else if(event.getChannel().equals("DeltaEss:TpHere"))
        {
            // Format: DestName/\DestServer
            String[] splitMessage = pattern.split(event.getMessage(), 2);
            String destName = splitMessage[0];
            String destServer = splitMessage[1];
            Player player = Bukkit.getPlayer(destName);

            if(player != null && player.isOnline())
            {
                deltaTeleport.sendToServer(player, destServer);
            }
        }
        else if(event.getChannel().equals("DeltaEss:Tpa"))
        {
            // Format: DestName/\SenderName/\DestServer
            String[] splitMessage = pattern.split(event.getMessage(), 3);
            String destName = splitMessage[0];
            String senderName = splitMessage[1];
            String destServer = splitMessage[2];
            Player destPlayer = Bukkit.getPlayer(destName);

            if(destPlayer != null && destPlayer.isOnline())
            {
                destPlayer.sendMessage(Prefixes.INFO + ChatColor.WHITE + senderName +
                    ChatColor.GRAY + " sent you a TP request. Use /tpaccept to accept.");
                deltaTeleport.addTpRequest(destName,
                    new TpRequest(senderName, destServer));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        TpRequest request = deltaTeleport.removeTpRequest(player.getName());

        if(request != null)
        {
            Player destPlayer = Bukkit.getPlayer(request.getSender());
            if(destPlayer != null && destPlayer.isOnline())
            {
                deltaTeleport.teleportWithEvent(player, destPlayer);
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event)
    {
        Player player = event.getPlayerToTeleport();
        Player dest = event.getDestination();

        if(dest.hasPermission("DeltaEss.Tp.Alert"))
        {
            dest.sendMessage(Prefixes.INFO + ChatColor.WHITE + player.getName() +
                ChatColor.GRAY + " teleported to you.");
        }
    }
}

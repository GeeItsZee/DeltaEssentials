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
package com.yahoo.tracebachi.DeltaEssentials.Teleportation.Commands;

import com.yahoo.tracebachi.DeltaEssentials.Teleportation.DeltaTeleport;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.TpListener;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class TpCommand implements TabExecutor
{
    private DeltaTeleport deltaTeleport;
    private DeltaRedisApi deltaRedisApi;

    public TpCommand(DeltaRedisApi deltaRedisApi, DeltaTeleport deltaTeleport)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.deltaTeleport = deltaTeleport;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
        this.deltaTeleport = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length != 0)
        {
            String lastArg = args[args.length - 1];
            return deltaRedisApi.matchStartOfName(lastArg);
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length == 0)
        {
            sender.sendMessage(Prefixes.INFO + "/tp <player>");
        }
        else if(args.length == 1 && sender.hasPermission("DeltaEss.Tp.Self"))
        {
            if(sender instanceof Player)
            {
                teleport((Player) sender, args[0]);
            }
            else
            {
                sender.sendMessage(Prefixes.FAILURE + "Only players can teleport to others.");
            }
        }
        else if(args.length >= 2 && sender.hasPermission("DeltaEss.Tp.Other"))
        {
            Player startPlayer = Bukkit.getPlayer(args[0]);
            if(startPlayer != null && startPlayer.isOnline())
            {
                teleport(startPlayer, args[1]);
            }
            else
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(args[0]) + " is not online.");
            }
        }
        else
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
        }
        return true;
    }

    private void teleport(Player playerToTp, String destName)
    {
        // Try to auto complete a partial name
        destName = attemptAutoComplete(playerToTp, destName);
        if(destName == null) { return; }

        // Check if the destination is online on the same server
        Player destination = Bukkit.getPlayer(destName);
        if(destination != null && destination.isOnline())
        {
            handleSameServerTeleport(playerToTp, destination);
        }
        else
        {
            handleDiffServerTeleport(playerToTp, destName);
        }
    }

    private String attemptAutoComplete(CommandSender sender, String partial)
    {
        List<String> partialMatches = deltaRedisApi.matchStartOfName(partial);
        if(!partialMatches.contains(partial.toLowerCase()))
        {
            if(partialMatches.size() == 0)
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(partial) +
                    " is not online.");
                return null;
            }
            else if(partialMatches.size() > 1)
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(partial) +
                    " matches too many players.");
                return null;
            }
            else
            {
                return partialMatches.get(0);
            }
        }
        return partial;
    }

    private void handleSameServerTeleport(Player playerToTp, Player destination)
    {
        String senderName = playerToTp.getName();
        String destName = destination.getName();

        if(playerToTp.canSee(destination) || playerToTp.hasPermission("DeltaEss.Tp.IgnoreVanish"))
        {
            if(deltaTeleport.isDenyingTp(destName) &&
                !playerToTp.hasPermission("DeltaEss.TpToggle.Bypass"))
            {
                playerToTp.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                    " is not allowing players to teleport to them.");
                destination.sendMessage(Prefixes.INFO + Prefixes.input(senderName) +
                    " tried to teleport to you, but you are denying teleports.");
            }
            else
            {
                deltaTeleport.teleportWithEvent(playerToTp, destination);
            }
        }
        else
        {
            playerToTp.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                " is not online.");
        }
    }

    private void handleDiffServerTeleport(Player playerToTp, String destName)
    {
        String senderName = playerToTp.getName();

        // Check other servers
        deltaRedisApi.findPlayer(destName, cachedPlayer ->
        {
            Player originalSender = Bukkit.getPlayer(senderName);
            if(originalSender != null && originalSender.isOnline())
            {
                if(cachedPlayer != null)
                {
                    // Format: TpSender/\DestName
                    String message = senderName.toLowerCase() + "/\\" +
                        destName.toLowerCase();

                    deltaRedisApi.publish(cachedPlayer.getServer(), TpListener.TP_CHANNEL, message);

                    originalSender.sendMessage(Prefixes.SUCCESS + "Teleporting ...");
                    deltaTeleport.sendToServer(playerToTp, cachedPlayer.getServer());
                }
                else
                {
                    originalSender.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                        " is not online.");
                }
            }
        });
    }
}

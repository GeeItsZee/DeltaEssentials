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
public class TpHereCommand implements TabExecutor
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaTeleport deltaTeleport;

    public TpHereCommand(DeltaRedisApi deltaRedisApi, DeltaTeleport deltaTeleport)
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
            sender.sendMessage(Prefixes.INFO + "/tphere <player>");
        }
        else if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can teleport others to themselves.");
        }
        else if(args.length >= 1 && sender.hasPermission("DeltaEss.Tp.Other"))
        {
            Player playerToTp = Bukkit.getPlayer(args[0]);
            if(playerToTp != null && playerToTp.isOnline())
            {
                deltaTeleport.teleportWithEvent(playerToTp, (Player) sender);
            }
            else
            {
                handleDiffServerTeleport(args[0], sender.getName());
            }
        }
        else
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
        }
        return true;
    }

    private void handleDiffServerTeleport(String nameToTp, String senderName)
    {
        deltaRedisApi.findPlayer(nameToTp, cachedPlayer ->
        {
            Player sender = Bukkit.getPlayer(senderName);
            if(sender != null && sender.isOnline())
            {
                if(cachedPlayer != null)
                {
                    // Format: NameToTp/\TpHereSender/\DestServer
                    String message = nameToTp.toLowerCase() + "/\\" +
                        senderName.toLowerCase() + "/\\" +
                        deltaRedisApi.getServerName();

                    deltaRedisApi.publish(cachedPlayer.getServer(), TpListener.TPHERE_CHANNEL, message);
                    deltaTeleport.addTpRequest(nameToTp, senderName);

                    sender.sendMessage(Prefixes.SUCCESS + "Teleporting here ...");
                }
                else
                {
                    sender.sendMessage(Prefixes.FAILURE + Prefixes.input(nameToTp) +
                        " is not online.");
                }
            }
        });
    }
}

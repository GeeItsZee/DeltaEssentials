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
public class TpaHereCommand implements TabExecutor
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaTeleport deltaTeleport;

    public TpaHereCommand(DeltaRedisApi deltaRedisApi, DeltaTeleport deltaTeleport)
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
            sender.sendMessage(Prefixes.INFO + "/tpahere player");
        }
        else if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can teleport others to themselves.");
        }
        else if(args.length >= 1 && sender.hasPermission("DeltaEss.Tpa.Send"))
        {
            String tpaReceiver = args[0];
            String tpaSender = sender.getName();
            Player destPlayer = Bukkit.getPlayer(tpaReceiver);

            if(destPlayer != null && destPlayer.isOnline())
            {
                destPlayer.sendMessage(Prefixes.INFO + Prefixes.input(tpaSender) +
                    " wants you to TP to them. Use /tpaccept to accept within 30 seconds.");
                deltaTeleport.addTpRequest(tpaReceiver, tpaSender);
                sender.sendMessage(Prefixes.SUCCESS + "Sent teleport request to player.");
            }
            else
            {
                handleDiffServerRequest(tpaReceiver, tpaSender);
            }
        }
        else
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
        }
        return true;
    }

    private void handleDiffServerRequest(String tpaReceiver, String tpaSender)
    {
        deltaRedisApi.findPlayer(tpaReceiver, cachedPlayer ->
        {
            Player sender = Bukkit.getPlayer(tpaSender);
            if(sender != null && sender.isOnline())
            {
                if(cachedPlayer != null)
                {
                    // Format: TpaReceiver/\TpaSender/\DestServer
                    String message = tpaReceiver.toLowerCase() + "/\\" +
                        tpaSender.toLowerCase() + "/\\" +
                        deltaRedisApi.getServerName();

                    deltaRedisApi.publish(cachedPlayer.getServer(), TpListener.TPAHERE_CHANNEL, message);
                    deltaTeleport.addTpRequest(tpaReceiver, tpaSender);

                    sender.sendMessage(Prefixes.SUCCESS + "Sent teleport request.");
                }
                else
                {
                    sender.sendMessage(Prefixes.FAILURE + Prefixes.input(tpaReceiver) +
                        " is not online.");
                }
            }
        });
    }
}

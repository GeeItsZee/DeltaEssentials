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

import com.google.common.base.Preconditions;
import com.yahoo.tracebachi.DeltaEssentials.Prefixes;
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.DeltaTeleport;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class TpHereCommand implements CommandExecutor
{
    private DeltaTeleport deltaTeleport;
    private DeltaRedisApi deltaRedisApi;

    public TpHereCommand(DeltaTeleport deltaTeleport, DeltaRedisApi deltaRedisApi)
    {
        this.deltaTeleport = deltaTeleport;
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
        this.deltaTeleport = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length == 0)
        {
            sender.sendMessage(Prefixes.INFO + "/tphere player");
        }
        else if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can teleport others to themselves.");
        }
        else if(args.length >= 1 && sender.hasPermission("DeltaEss.Tp.Other"))
        {
            teleportHere((Player) sender, args[0]);
        }
        else
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
        }
        return true;
    }

    private void teleportHere(Player sender, String nameToTp)
    {
        Preconditions.checkNotNull(sender, "Sender cannot be null.");
        Preconditions.checkNotNull(nameToTp, "Name to TP cannot be null.");

        Player playerToTp = Bukkit.getPlayer(nameToTp);
        if(playerToTp != null && playerToTp.isOnline())
        {
            deltaTeleport.teleportWithEvent(playerToTp, sender);
            return;
        }

        // Check other servers
        String senderName = sender.getName();
        deltaRedisApi.findPlayer(nameToTp, cachedPlayer -> {

            Player originalSender = Bukkit.getPlayer(senderName);
            if(originalSender != null && originalSender.isOnline())
            {
                if(cachedPlayer != null)
                {
                    // Format: DestName/\DestServer
                    String message = nameToTp.toLowerCase() + "/\\" +
                        deltaRedisApi.getServerName();

                    deltaRedisApi.publish(cachedPlayer.getServer(), "DeltaEss:TpHere", message);
                    deltaTeleport.addTpRequest(nameToTp, sender.getName());

                    originalSender.sendMessage(Prefixes.SUCCESS + "Teleporting player here ...");
                }
                else
                {
                    originalSender.sendMessage(Prefixes.FAILURE + "Player not found.");
                }
            }
        });
    }
}

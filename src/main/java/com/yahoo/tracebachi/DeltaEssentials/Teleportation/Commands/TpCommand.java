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
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class TpCommand implements CommandExecutor
{
    private DeltaTeleport deltaTeleport;
    private DeltaRedisApi deltaRedisApi;

    public TpCommand(DeltaTeleport deltaTeleport, DeltaRedisApi deltaRedisApi)
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
            sender.sendMessage(Prefixes.INFO + "/tp player");
        }
        else if(args.length == 1 && sender.hasPermission("DeltaEss.Tp.Self"))
        {
            if(!(sender instanceof Player))
            {
                sender.sendMessage(Prefixes.FAILURE + "Only players can teleport to others.");
            }
            else
            {
                teleport((Player) sender, args[0]);
            }
        }
        else if(args.length >= 2 && sender.hasPermission("DeltaEss.Tp.Other"))
        {
            Player startPlayer = Bukkit.getPlayer(args[0]);

            if(startPlayer == null || !startPlayer.isOnline())
            {
                sender.sendMessage(Prefixes.FAILURE + ChatColor.WHITE + args[0] +
                    ChatColor.GRAY + " is not online.");
            }
            else
            {
                teleport(startPlayer, args[1]);
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
        Preconditions.checkNotNull(playerToTp, "Player to TP cannot be null.");
        Preconditions.checkNotNull(destName, "Destination cannot be null.");

        Player destPlayer = Bukkit.getPlayer(destName);
        if(destPlayer != null && destPlayer.isOnline())
        {
            if(playerToTp.canSee(destPlayer))
            {
                deltaTeleport.teleportWithEvent(playerToTp, destPlayer);
            }
            else
            {
                playerToTp.sendMessage(Prefixes.FAILURE + "Player not found.");
            }
            return;
        }

        // Check other servers
        String senderName = playerToTp.getName();
        deltaRedisApi.findPlayer(destName, cachedPlayer -> {

            Player originalSender = Bukkit.getPlayer(senderName);
            if(originalSender != null && originalSender.isOnline())
            {
                if(cachedPlayer != null)
                {
                    // Format: SenderName/\DestName
                    String message = playerToTp.getName().toLowerCase() + "/\\" +
                        destName.toLowerCase();

                    deltaRedisApi.publish(cachedPlayer.getServer(), "DeltaEss:Tp", message);
                    deltaTeleport.sendToServer(playerToTp, cachedPlayer.getServer());

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

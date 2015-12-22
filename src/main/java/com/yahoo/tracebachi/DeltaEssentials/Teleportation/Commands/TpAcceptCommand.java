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
import com.yahoo.tracebachi.DeltaEssentials.Teleportation.TpRequest;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class TpAcceptCommand implements CommandExecutor
{
    private DeltaTeleport deltaTeleport;

    public TpAcceptCommand(DeltaTeleport deltaTeleport)
    {
        this.deltaTeleport = deltaTeleport;
    }

    public void shutdown()
    {
        this.deltaTeleport = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can accept teleports.");
        }
        else if(sender.hasPermission("DeltaEss.Tpa.Accept"))
        {
            TpRequest request = deltaTeleport.getTpRequest(sender.getName());
            Player player = (Player) sender;

            if(request == null)
            {
                sender.sendMessage(Prefixes.FAILURE + "Request not found.");
            }
            else if(request.getServer() == null)
            {
                Player destPlayer = Bukkit.getPlayer(request.getSender());
                if(destPlayer != null && destPlayer.isOnline())
                {
                    deltaTeleport.teleportWithEvent(player, destPlayer);
                    sender.sendMessage(Prefixes.SUCCESS + "Teleporting ...");
                }
                else
                {
                    sender.sendMessage(Prefixes.FAILURE + "The sender is no longer online on this server.");
                }
            }
            else
            {
                sender.sendMessage(Prefixes.SUCCESS + "Teleporting ...");
                deltaTeleport.sendToServer(player, request.getServer());
            }
        }
        else
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
        }
        return true;
    }
}

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
package com.yahoo.tracebachi.DeltaEssentials;

import com.yahoo.tracebachi.DeltaRedis.Shared.Channels;
import com.yahoo.tracebachi.DeltaRedis.Shared.Interfaces.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/4/15.
 */
public class MoveToCommand implements CommandExecutor
{
    private DeltaEssentialsPlugin essentialsPlugin;
    private DeltaRedisApi deltaRedisApi;

    public MoveToCommand(DeltaEssentialsPlugin essentialsPlugin, DeltaRedisApi deltaRedisApi)
    {
        this.essentialsPlugin = essentialsPlugin;
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
        this.essentialsPlugin = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        Set<String> servers = new HashSet<>();
        Set<String> blockedServers = essentialsPlugin.getBlockedServers();
        String currentServer = deltaRedisApi.getServerName();

        for(String serverName : deltaRedisApi.getServers())
        {
            if(!serverName.equals(Channels.BUNGEECORD))
            {
                servers.add(serverName.toLowerCase());
            }
        }

        if(args.length == 0)
        {
            sender.sendMessage(Prefixes.INFO + "You are currently in " +
                ChatColor.WHITE + currentServer);
            sender.sendMessage(Prefixes.INFO + "/moveto server [player]");

            List<String> sorted = Arrays.asList(servers.toArray(new String[servers.size()]));
            Collections.sort(sorted);
            String joined = String.join(ChatColor.GRAY + ", " + ChatColor.WHITE, sorted);

            sender.sendMessage(Prefixes.INFO + "Online servers: " + ChatColor.WHITE + joined);
            return true;
        }

        if(args.length == 1)
        {
            if(!(sender instanceof Player))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "Consoles cannot move servers.");
            }
            else if(!sender.hasPermission("DeltaEss.MoveTo.Self"))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "You do not have permission to use that command.");
            }
            else if(currentServer.equalsIgnoreCase(args[0]))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "You are already connected to that server.");
            }
            else if(blockedServers.contains(args[0].toLowerCase()) &&
                !sender.hasPermission("DeltaEss.MoveTo.Bypass"))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "That server is blocked and you do not have permission to bypass it.");
            }
            else if(!servers.contains(args[0].toLowerCase()))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "There is no server online named " + ChatColor.WHITE + args[0]);
            }
            else
            {
                sender.sendMessage(Prefixes.SUCCESS +
                    "Attempting to switch servers ...");
                essentialsPlugin.sendToServer((Player) sender, args[0].toLowerCase());
            }
        }
        else
        {
            if(!sender.hasPermission("DeltaEss.MoveTo.Other"))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "You do not have permission to use that command.");
            }
            else if(currentServer.equalsIgnoreCase(args[0]))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "You are already connected to that server.");
            }
            else if(blockedServers.contains(args[0].toLowerCase()) &&
                !sender.hasPermission("DeltaEss.MoveTo.Bypass"))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "That server is blocked and you do not have permission to bypass it.");
            }
            else if(!servers.contains(args[0].toLowerCase()))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "There is no server online named " + ChatColor.WHITE + args[0]);
            }
            else
            {
                Player target = Bukkit.getPlayer(args[1]);

                if(target == null || !target.isOnline())
                {
                    sender.sendMessage(Prefixes.FAILURE +
                        "That player is not online.");
                }
                else
                {
                    sender.sendMessage(Prefixes.SUCCESS +
                        "Attempting to switch servers ...");
                    essentialsPlugin.sendToServer(target, args[0].toLowerCase());
                }
            }
        }

        return true;
    }
}

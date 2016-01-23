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
package com.gmail.tracebachi.DeltaEssentials.Commands;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsChannels;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Utils.CallbackUtil;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandMoveTo extends DeltaEssentialsCommand
{
    private DeltaRedisApi deltaRedisApi;

    public CommandMoveTo(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super("moveto", null, plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void shutdown()
    {
        deltaRedisApi = null;
        super.shutdown();
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args)
    {
        String lastArg = args[args.length - 1].toLowerCase();
        return deltaRedisApi.matchStartOfServerName(lastArg);
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        Settings settings = plugin.getSettings();
        Set<String> servers = deltaRedisApi.getCachedServers();
        String currentServer = deltaRedisApi.getServerName().toLowerCase();

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "You are currently in " +
                Prefixes.input(currentServer));
            sender.sendMessage(Prefixes.INFO + "/moveto <server>");
            sender.sendMessage(Prefixes.INFO + "Online servers: " +
                getFormattedServerList(servers));
            return;
        }

        String destServer = args[0];
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
            else if(currentServer.equalsIgnoreCase(destServer))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "You are already connected to that server.");
            }
            else if(!isOnline(servers, destServer))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "There is no server online named " + Prefixes.input(destServer));
            }
            else if(settings.isServerBlocked(destServer) &&
                !sender.hasPermission("DeltaEss.BlockedServerBypass"))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "That server is blocked and you do not have permission to bypass it.");
            }
            else
            {
                sender.sendMessage(Prefixes.SUCCESS + "Attempting to switch servers ...");
                plugin.getPlayerDataIOListener().savePlayerData((Player) sender, destServer);
            }
        }
        else
        {
            if(!sender.hasPermission("DeltaEss.MoveTo.Other"))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "You do not have permission to use that command.");
            }
            else if(currentServer.equalsIgnoreCase(destServer))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "You are already connected to that server.");
            }
            else if(!isOnline(servers, destServer))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "There is no server online named " + Prefixes.input(destServer));
            }
            else if(settings.isServerBlocked(destServer) &&
                !sender.hasPermission("DeltaEss.BlockedServerBypass"))
            {
                sender.sendMessage(Prefixes.FAILURE +
                    "That server is blocked and you do not have permission to bypass it.");
            }
            else
            {
                String senderName = sender.getName();
                String targetName = args[1].toLowerCase();
                Player target = Bukkit.getPlayer(targetName);

                if(target != null && target.isOnline())
                {
                    sender.sendMessage(Prefixes.SUCCESS + "Attempting to switch servers ...");
                    plugin.getPlayerDataIOListener().savePlayerData(target, destServer);
                }
                else
                {
                    handlePlayerInDiffServer(senderName, destServer, targetName);
                }
            }
        }
    }

    private void handlePlayerInDiffServer(String senderName, String destServer, String targetName)
    {
        deltaRedisApi.findPlayer(targetName, cachedPlayer ->
        {
            if(cachedPlayer == null)
            {
                CallbackUtil.sendMessage(senderName,
                    Prefixes.FAILURE + "Player not found.");
                return;
            }

            if(!cachedPlayer.getServer().equalsIgnoreCase(destServer))
            {
                CallbackUtil.sendMessage(senderName,
                    Prefixes.FAILURE + "Player is already in that server.");
                return;
            }

            deltaRedisApi.publish(cachedPlayer.getServer(),
                DeltaEssentialsChannels.MOVE,
                senderName + "/\\" + targetName + "/\\" + destServer);

            CallbackUtil.sendMessage(senderName,
                Prefixes.SUCCESS + "Sending player to " + destServer);
        });
    }

    private String getFormattedServerList(Set<String> servers)
    {
        List<String> serverList = new ArrayList<>(servers.size());
        serverList.addAll(servers.stream().collect(Collectors.toList()));

        Collections.sort(serverList);
        return ChatColor.WHITE + String.join(ChatColor.GRAY + ", " + ChatColor.WHITE, serverList);
    }

    private boolean isOnline(Set<String> onlineSet, String input)
    {
        for(String serverName : onlineSet)
        {
            if(serverName.toLowerCase().equalsIgnoreCase(input))
            {
                return true;
            }
        }
        return false;
    }
}

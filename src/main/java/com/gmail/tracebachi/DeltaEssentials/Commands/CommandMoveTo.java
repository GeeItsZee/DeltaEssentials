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
import com.gmail.tracebachi.DeltaEssentials.Utils.CommandMessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent.DELTA_PATTERN;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandMoveTo implements TabExecutor, Registerable, Shutdownable, Listener
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentials plugin;

    public CommandMoveTo(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("moveto").setExecutor(this);
        plugin.getCommand("moveto").setTabCompleter(this);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("moveto").setExecutor(null);
        plugin.getCommand("moveto").setTabCompleter(null);

        HandlerList.unregisterAll(this);
    }

    @Override
    public void shutdown()
    {
        unregister();
        deltaRedisApi = null;
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args)
    {
        String lastArg = args[args.length - 1].toLowerCase();
        return deltaRedisApi.matchStartOfServerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        Set<String> servers = deltaRedisApi.getCachedServers();
        String currentServer = deltaRedisApi.getServerName();
        String destServer = args[0];
        Settings settings = plugin.getSettings();

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "Currently in " +
                Prefixes.input(currentServer));
            sender.sendMessage(Prefixes.INFO + "Online servers: " +
                getFormattedServerList(servers));
            return true;
        }

        if(!servers.contains(destServer))
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(destServer) +
                " is offline or non-existent.");
            return true;
        }

        if(settings.isServerBlocked(destServer) &&
            !sender.hasPermission("DeltaEss.BlockedServerBypass"))
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(destServer) +
                " is blocked.");
            return true;
        }

        if(args.length == 1)
        {
            handleSelfMoveTo(sender, currentServer, destServer);
        }
        else
        {
            handleOtherMoveTo(sender, args[1], currentServer, destServer);
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onRedisMessageEvent(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.MOVE))
        {
            String[] split = DELTA_PATTERN.split(event.getMessage(), 3);
            String sender = split[0];
            String nameToMove = split[1];
            String destination = split[2];

            Player toMove = Bukkit.getPlayer(nameToMove);

            if(toMove != null)
            {
                plugin.info(sender + " is moving " + nameToMove + " to " + destination);
                plugin.sendToServer(toMove, destination);
            }
            else
            {
                deltaRedisApi.sendMessageToPlayer(sender, Prefixes.FAILURE +
                    Prefixes.input(nameToMove) + " is not online.");
            }
        }
    }

    private void handleSelfMoveTo(CommandSender sender, String currentServer, String destServer)
    {
        if(!(sender instanceof Player))
        {
            CommandMessageUtil.onlyForPlayers(sender, "moveto <server>");
            return;
        }

        if(!sender.hasPermission("DeltaEss.MoveTo.Self"))
        {
            CommandMessageUtil.noPermission(sender, "DeltaEss.MoveTo.Self");
            return;
        }

        if(currentServer.equalsIgnoreCase(destServer))
        {
            sender.sendMessage(Prefixes.FAILURE + "You are already connected to " +
                Prefixes.input(currentServer));
            return;
        }

        handlePlayerInSameServer(((Player) sender), destServer);
    }

    private void handleOtherMoveTo(CommandSender sender, String targetName, String currentServer,
        String destServer)
    {
        String senderName = sender.getName();
        Player target = Bukkit.getPlayer(targetName);

        if(!sender.hasPermission("DeltaEss.MoveTo.Other"))
        {
            CommandMessageUtil.noPermission(sender, "DeltaEss.MoveTo.Other");
            return;
        }

        sender.sendMessage(Prefixes.SUCCESS + "Sending " +
            Prefixes.input(targetName) + " to " +
            Prefixes.input(destServer) + " ...");

        if(target == null)
        {
            handlePlayerInDiffServer(senderName, destServer, targetName);
            return;
        }

        if(destServer.equals(currentServer))
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(targetName) +
                " is already on " + Prefixes.input(destServer));
            return;
        }

        handlePlayerInSameServer(target, destServer);
    }

    private void handlePlayerInSameServer(Player player, String destServer)
    {
        player.sendMessage(Prefixes.SUCCESS + "Moving to " +
            Prefixes.input(destServer) + " ...");

        plugin.sendToServer(player, destServer);
    }

    private void handlePlayerInDiffServer(String senderName, String destServer, String targetName)
    {
        deltaRedisApi.findPlayer(targetName, cachedPlayer ->
        {
            if(cachedPlayer == null)
            {
                CommandMessageUtil.playerOffline(senderName, targetName);
                return;
            }

            // Format: SenderName/\TargetName/\DestServer
            deltaRedisApi.publish(cachedPlayer.getServer(),
                DeltaEssentialsChannels.MOVE,
                senderName, targetName, destServer);
        });
    }

    private String getFormattedServerList(Set<String> servers)
    {
        List<String> serverList = new ArrayList<>(servers);
        Collections.sort(serverList);
        return ChatColor.WHITE + String.join(ChatColor.GRAY + ", " + ChatColor.WHITE, serverList);
    }
}

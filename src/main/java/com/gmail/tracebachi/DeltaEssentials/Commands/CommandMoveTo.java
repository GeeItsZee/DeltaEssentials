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
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.SplitPatterns;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
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

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandMoveTo implements TabExecutor, Registerable, Shutdownable, Listener
{
    private DeltaEssentials plugin;

    public CommandMoveTo(DeltaEssentials plugin)
    {
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
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command,
                                      String s, String[] args)
    {
        String lastArg = args[args.length - 1].toLowerCase();
        return DeltaRedisApi.instance().matchStartOfServerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        DeltaRedisApi api = DeltaRedisApi.instance();
        Set<String> servers = api.getCachedServers();
        String currentServer = api.getServerName();

        if(args.length < 1)
        {
            String formattedList = getFormattedServerList(servers);

            sender.sendMessage(Settings.format("CurrentServer", currentServer));
            sender.sendMessage(Settings.format("OnlineServerList", formattedList));
            return true;
        }

        String destServer = getMatchInSet(servers, args[0]);

        if(destServer == null)
        {
            sender.sendMessage(Settings.format("ServerOffline", args[0]));
            return true;
        }

        // TODO BlockedServerBypass.<server name>
        if(Settings.isServerBlocked(destServer) &&
            !sender.hasPermission("DeltaEss.BlockedServerBypass"))
        {
            sender.sendMessage(Settings.format("BlockedServer", destServer));
            return true;
        }

        if(args.length == 1)
        {
            handleMoveSelfTo(sender, currentServer, destServer);
        }
        else
        {
            handleMoveOtherTo(sender, currentServer, destServer, args[1]);
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRedisMessageEvent(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.MOVE))
        {
            String[] split = SplitPatterns.DELTA.split(event.getMessage(), 3);
            String sender = split[0];
            String nameToMove = split[1];
            String destination = split[2];

            Player toMove = Bukkit.getPlayerExact(nameToMove);

            if(toMove == null)
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sender,
                    Settings.format("PlayerOffline", nameToMove));
                return;
            }

            plugin.info(sender + " is moving " + nameToMove + " to " + destination);
            handlePlayerInSameServer(toMove, destination);
        }
    }

    private void handleMoveSelfTo(CommandSender sender, String currentServer, String destServer)
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage(Settings.format("PlayersOnly", "/moveto <server>"));
            return;
        }

        if(!sender.hasPermission("DeltaEss.MoveTo.Self"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaEss.MoveTo.Self"));
            return;
        }

        if(currentServer.equalsIgnoreCase(destServer))
        {
            sender.sendMessage(Settings.format("InputIsCurrentServer", currentServer));
            return;
        }

        handlePlayerInSameServer(((Player) sender), destServer);
    }

    private void handleMoveOtherTo(CommandSender sender, String currentServer, String destServer,
                                   String targetName)
    {
        if(!sender.hasPermission("DeltaEss.MoveTo.Other"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaEss.MoveTo.Other"));
            return;
        }

        String senderName = sender.getName();
        Player target = Bukkit.getPlayerExact(targetName);

        if(target == null)
        {
            handlePlayerInDiffServer(senderName, destServer, targetName);
            return;
        }

        if(destServer.equals(currentServer))
        {
            sender.sendMessage(Settings.format("OtherAlreadyOnServer", targetName, destServer));
            return;
        }

        sender.sendMessage(Settings.format("MovingOtherToMessage", targetName, destServer));

        handlePlayerInSameServer(target, destServer);
    }

    private void handlePlayerInSameServer(Player player, String destServer)
    {
        player.sendMessage(Settings.format("MovingToMessage", destServer));

        plugin.sendToServer(player, destServer);
    }

    private void handlePlayerInDiffServer(String senderName, String destServer, String targetName)
    {
        DeltaRedisApi.instance().findPlayer(targetName, cachedPlayer ->
        {
            if(cachedPlayer == null)
            {
                MessageUtil.sendMessage(
                    senderName,
                    Settings.format("PlayerOffline", targetName));
                return;
            }

            if(cachedPlayer.getServer().equals(destServer))
            {
                MessageUtil.sendMessage(
                    senderName,
                    Settings.format("OtherAlreadyOnServer", targetName, destServer));
                return;
            }

            MessageUtil.sendMessage(
                senderName,
                Settings.format("MovingOtherToMessage", targetName, destServer));

            // Format: SenderName/\TargetName/\DestServer
            DeltaRedisApi.instance().publish(
                cachedPlayer.getServer(),
                DeltaEssentialsChannels.MOVE,
                senderName,
                targetName,
                destServer);
        });
    }

    private String getFormattedServerList(Set<String> servers)
    {
        List<String> serverList = new ArrayList<>(servers.size());

        for(String server : servers)
        {
            if(!Settings.isServerBlocked(server))
            {
                serverList.add(server);
            }
        }

        Collections.sort(serverList);
        return String.join(", ", serverList);
    }

    private String getMatchInSet(Set<String> set, String source)
    {
        for(String item : set)
        {
            if(item.equalsIgnoreCase(source))
            {
                return item;
            }
        }
        return null;
    }
}

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
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashSet;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;
import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.formatNoPerm;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandMoveAll implements TabExecutor, Registerable, Shutdownable, Listener
{
    private Settings settings;
    private DeltaEssentials plugin;

    public CommandMoveAll(Settings settings, DeltaEssentials plugin)
    {
        Preconditions.checkNotNull(settings, "settings");
        Preconditions.checkNotNull(plugin, "plugin");

        this.settings = settings;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("moveall").setExecutor(this);
        plugin.getCommand("moveall").setTabCompleter(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("moveall").setExecutor(null);
        plugin.getCommand("moveall").setTabCompleter(null);
        HandlerList.unregisterAll(this);
    }

    @Override
    public void shutdown()
    {
        unregister();
        settings = null;
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command,
                                      String s, String[] args)
    {
        String lastArg = args[args.length - 1].toLowerCase();
        List<String> serverList = DeltaRedisApi.instance().matchStartOfServerName(lastArg);
        List<String> resultList = new ArrayList<>(serverList.size());
        CaseInsensitiveHashSet privateServers = settings.getPrivateServers();

        for(String server : serverList)
        {
            if(!privateServers.contains(server))
            {
                resultList.add(server);
            }
        }

        return resultList;
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

            sender.sendMessage(format("DeltaEss.CurrentServer", currentServer));
            sender.sendMessage(format("DeltaEss.OnlineServerList", formattedList));
            return true;
        }

        if(!sender.hasPermission("DeltaEss.MoveAll"))
        {
            sender.sendMessage(formatNoPerm("DeltaEss.MoveAll"));
            return true;
        }

        String destServer = getMatchInSet(servers, args[0]);

        if(destServer == null)
        {
            sender.sendMessage(format("DeltaEss.ServerOffline", args[0]));
            return true;
        }

        if(currentServer.equalsIgnoreCase(destServer))
        {
            sender.sendMessage(format("DeltaEss.InputIsCurrentServer", destServer));
            return true;
        }

        for(Player player : Bukkit.getOnlinePlayers())
        {
            player.sendMessage(format("DeltaEss.MovingToMessage", destServer));
            plugin.sendToServer(player, destServer);
        }

        return true;
    }

    private String getFormattedServerList(Set<String> servers)
    {
        List<String> serverList = new ArrayList<>(servers.size());
        CaseInsensitiveHashSet privateServers = settings.getPrivateServers();

        for(String server : servers)
        {
            if(!privateServers.contains(server))
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

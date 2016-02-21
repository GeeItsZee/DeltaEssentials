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
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
import com.gmail.tracebachi.DeltaEssentials.Utils.CommandMessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTpAccept implements TabExecutor, Registerable, Shutdownable
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentials plugin;

    public CommandTpAccept(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("tpaccept").setExecutor(this);
        plugin.getCommand("tpaccept").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("tpaccept").setExecutor(null);
        plugin.getCommand("tpaccept").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        deltaRedisApi = null;
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfPlayerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!(sender instanceof Player))
        {
            CommandMessageUtil.onlyForPlayers(sender, "tpaccept");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Tpa.Accept"))
        {
            CommandMessageUtil.noPermission(sender, "DeltaEss.Tpa.Accept");
            return true;
        }

        TeleportRequest request = plugin.getTeleportListener().getRequestMap()
            .get(sender.getName());

        if(request == null)
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have a pending TPA request.");
            return true;
        }

        Player player = (Player) sender;
        String currentServerName = deltaRedisApi.getServerName();
        String destServerName = request.getDestServer();

        if(!destServerName.equals(currentServerName))
        {
            plugin.sendToServer(player, destServerName);
            return true;
        }

        Player destPlayer = Bukkit.getPlayer(request.getSender());

        if(destPlayer != null)
        {
            plugin.getTeleportListener().teleport(player, destPlayer);
        }
        else
        {
            CommandMessageUtil.playerOffline(sender, request.getSender());
        }

        return true;
    }
}

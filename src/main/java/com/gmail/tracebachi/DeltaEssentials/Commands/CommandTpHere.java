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
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTpEvent;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
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
public class CommandTpHere implements TabExecutor, Registerable, Shutdownable
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentials plugin;

    public CommandTpHere(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("tphere").setExecutor(this);
        plugin.getCommand("tphere").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("tphere").setExecutor(null);
        plugin.getCommand("tphere").setTabCompleter(null);
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
        if(args.length < 1)
        {
            sender.sendMessage(Settings.format("TeleportHereUsage"));
            return true;
        }

        if(!(sender instanceof Player))
        {
            sender.sendMessage(Settings.format("PlayersOnly", "/tphere"));
            return true;
        }

        if(!sender.hasPermission("DeltaEss.TpOther"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaEss.TpOther"));
            return true;
        }

        DeltaEssPlayerData playerData = plugin.getPlayerMap().get(sender.getName());

        if(playerData == null)
        {
            sender.sendMessage(Settings.format("PlayerDataNotLoaded"));
            return true;
        }

        String senderName = sender.getName();
        String toTpName = args[0];
        Player toTp = Bukkit.getPlayer(toTpName);

        if(toTp != null)
        {
            plugin.getTeleportListener().teleport(toTp, (Player) sender,
                PlayerTpEvent.TeleportType.TP_HERE);

            return true;
        }

        deltaRedisApi.findPlayer(toTpName, cachedPlayer ->
        {
            Player sendingPlayer = Bukkit.getPlayer(senderName);

            if(sendingPlayer == null) return;

            if(cachedPlayer == null)
            {
                sendingPlayer.sendMessage(Settings.format("PlayerOffline", toTpName));
                return;
            }

            String destServer = cachedPlayer.getServer();
            String currentServer = deltaRedisApi.getServerName();
            TeleportRequest request = new TeleportRequest(senderName, currentServer,
                PlayerTpEvent.TeleportType.TP_HERE);

            // Format: Receiver/\Sender/\CurrentServer
            deltaRedisApi.publish(destServer, DeltaEssentialsChannels.TP_HERE,
                toTpName, senderName, currentServer);

            plugin.getTeleportListener().getRequestMap().put(toTpName, request);
        });

        return true;
    }
}

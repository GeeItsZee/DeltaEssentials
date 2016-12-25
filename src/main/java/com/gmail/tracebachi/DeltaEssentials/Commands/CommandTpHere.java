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
import com.gmail.tracebachi.DeltaEssentials.Listeners.TeleportListener;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTpHere implements TabExecutor, Registerable, Shutdownable
{
    private TeleportListener teleportListener;
    private DeltaEssentials plugin;

    public CommandTpHere(TeleportListener teleportListener, DeltaEssentials plugin)
    {
        this.teleportListener = teleportListener;
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
        teleportListener = null;
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return DeltaRedisApi.instance().matchStartOfPlayerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(format("/tphere <name>"));
            return true;
        }

        if(!(sender instanceof Player))
        {
            sender.sendMessage(formatPlayerOnlyCommand("/tphere <name>"));
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Tp.Other"))
        {
            sender.sendMessage(formatNoPerm("DeltaEss.Tp.Other"));
            return true;
        }

        DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(sender.getName());
        if(playerData == null)
        {
            sender.sendMessage(format("DeltaEss.PlayerDataNotLoaded"));
            return true;
        }

        String toTpName = args[0];
        Player toTp = Bukkit.getPlayerExact(toTpName);
        if(toTp != null)
        {
            teleportListener.teleport(
                toTp,
                (Player) sender,
                PlayerTpEvent.TeleportType.TP_HERE);
            return true;
        }

        String senderName = sender.getName();
        DeltaRedisApi.instance().findPlayer(toTpName, cachedPlayer ->
        {
            Player sendingPlayer = Bukkit.getPlayerExact(senderName);

            if(sendingPlayer == null) { return; }

            if(cachedPlayer == null)
            {
                sendingPlayer.sendMessage(formatPlayerOffline(toTpName));
                return;
            }

            DeltaRedisApi deltaRedisApi = DeltaRedisApi.instance();
            String destServer = cachedPlayer.getServer();
            String currentServer = deltaRedisApi.getServerName();
            TeleportRequest request = new TeleportRequest(
                senderName,
                currentServer,
                PlayerTpEvent.TeleportType.TP_HERE);

            deltaRedisApi.publish(
                destServer,
                DeltaEssentialsChannels.TP_HERE,
                senderName,
                toTpName,
                currentServer);

            teleportListener.getRequestMap().put(toTpName, request);
        });

        return true;
    }
}

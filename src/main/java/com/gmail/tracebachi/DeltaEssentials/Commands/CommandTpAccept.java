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
public class CommandTpAccept implements TabExecutor, Registerable, Shutdownable
{
    private TeleportListener teleportListener;
    private DeltaEssentials plugin;

    public CommandTpAccept(TeleportListener teleportListener, DeltaEssentials plugin)
    {
        this.teleportListener = teleportListener;
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
        if(!(sender instanceof Player))
        {
            sender.sendMessage(formatUsage("/tpaccept"));
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Tpa.Accept"))
        {
            sender.sendMessage(formatNoPerm("DeltaEss.Tpa.Accept"));
            return true;
        }

        String senderName = sender.getName();

        DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(senderName);
        if(playerData == null)
        {
            sender.sendMessage(format("DeltaEss.PlayerDataNotLoaded"));
            return true;
        }

        TeleportRequest request = teleportListener.getRequestMap().get(senderName);
        if(request == null)
        {
            sender.sendMessage(format("DeltaEss.NoPendingTeleportRequest"));
            return true;
        }

        Player player = (Player) sender;
        String currentServerName = DeltaRedisApi.instance().getServerName();

        String destServerName = request.getDestServer();
        if(!destServerName.equals(currentServerName))
        {
            plugin.sendToServer(player, destServerName);
            return true;
        }

        Player destPlayer = Bukkit.getPlayerExact(request.getSender());
        if(destPlayer == null)
        {
            sender.sendMessage(formatPlayerOffline(request.getSender()));
            return true;
        }

        teleportListener.teleport(
            player,
            destPlayer,
            PlayerTpEvent.TeleportType.TPA_HERE);
        return true;
    }
}

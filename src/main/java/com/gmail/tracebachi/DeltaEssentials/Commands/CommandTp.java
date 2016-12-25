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
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtil;
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
public class CommandTp implements TabExecutor, Registerable, Shutdownable
{
    private TeleportListener teleportListener;
    private DeltaEssentials plugin;

    public CommandTp(TeleportListener teleportListener, DeltaEssentials plugin)
    {
        this.teleportListener = teleportListener;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("tp").setExecutor(this);
        plugin.getCommand("tp").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("tp").setExecutor(null);
        plugin.getCommand("tp").setTabCompleter(null);
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
            sender.sendMessage(formatUsage("/tp <name>"));
            return true;
        }

        if(args.length == 1)
        {
            if(!(sender instanceof Player))
            {
                sender.sendMessage(formatPlayerOnlyCommand("/tp <name>"));
                return true;
            }

            if(!sender.hasPermission("DeltaEss.Tp.Self"))
            {
                sender.sendMessage(formatNoPerm("DeltaEss.Tp.Self"));
                return true;
            }

            DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(sender.getName());

            if(playerData == null)
            {
                sender.sendMessage(format("DeltaEss.PlayerDataNotLoaded"));
                return true;
            }

            handleTeleport((Player) sender, args[0]);
        }
        else
        {
            String firstName = args[0];
            String secondName = args[1];
            Player firstPlayer = Bukkit.getPlayerExact(firstName);

            if(firstPlayer == null)
            {
                sender.sendMessage(formatPlayerOffline(firstName));
                return true;
            }

            if(!sender.hasPermission("DeltaEss.Tp.Other"))
            {
                sender.sendMessage(formatNoPerm("DeltaEss.Tp.Other"));
                return true;
            }

            DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(firstName);

            if(playerData == null)
            {
                sender.sendMessage(format("DeltaEss.PlayerDataNotLoaded"));
                return true;
            }

            handleTeleport(firstPlayer, secondName);
        }

        return true;
    }

    private void handleTeleport(Player toTp, String destName)
    {
        String toTpName = toTp.getName();
        String autoCompletedDestName = attemptAutoComplete(destName);

        if(autoCompletedDestName == null)
        {
            toTp.sendMessage(format("DeltaEss.TooManyAutoCompleteMatches", destName));
            return;
        }

        Player destination = Bukkit.getPlayerExact(autoCompletedDestName);
        if(destination != null)
        {
            teleportListener.teleport(
                toTp,
                destination,
                PlayerTpEvent.TeleportType.NORMAL_TP);
            return;
        }

        DeltaRedisApi.instance().findPlayer(destName, cachedPlayer ->
        {
            Player player = Bukkit.getPlayerExact(toTpName);

            if(player == null) { return; }

            if(cachedPlayer == null)
            {
                MessageUtil.sendMessage(
                    toTpName,
                    format("DeltaEss.PlayerOffline", destName));
                return;
            }

            String destServer = cachedPlayer.getServer();
            DeltaRedisApi.instance().publish(
                destServer,
                DeltaEssentialsChannels.TP,
                toTpName,
                destName);

            plugin.sendToServer(player, destServer);
        });
    }

    private String attemptAutoComplete(String partial)
    {
        List<String> partialMatches = DeltaRedisApi.instance().matchStartOfPlayerName(partial);

        if(partialMatches.contains(partial.toLowerCase()))
        {
            return partial;
        }

        if(partialMatches.size() == 0)
        {
            return partial;
        }

        if(partialMatches.size() == 1)
        {
            return partialMatches.get(0);
        }

        return null;
    }
}

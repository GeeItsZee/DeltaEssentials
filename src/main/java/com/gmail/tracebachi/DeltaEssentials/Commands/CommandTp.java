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
public class CommandTp implements TabExecutor, Registerable, Shutdownable
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentials plugin;

    public CommandTp(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
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
            sender.sendMessage(Prefixes.INFO + "/tp <player>");
            return true;
        }

        if(args.length == 1)
        {
            if(!(sender instanceof Player))
            {
                CommandMessageUtil.onlyForPlayers(sender, "tp <player>");
            }
            else if(!sender.hasPermission("DeltaEss.Tp"))
            {
                CommandMessageUtil.noPermission(sender, "DeltaEss.Tp");
            }
            else
            {
                String destName = args[0];
                handleTeleport((Player) sender, destName);
            }
        }
        else
        {
            String firstName = args[0];
            String secondName = args[1];
            Player firstPlayer = Bukkit.getPlayer(firstName);

            if(firstPlayer == null)
            {
                CommandMessageUtil.playerOffline(sender, firstName);
            }
            else if(!sender.hasPermission("DeltaEss.TpOther"))
            {
                CommandMessageUtil.noPermission(sender, "DeltaEss.TpOther");
            }
            else
            {
                handleTeleport(firstPlayer, secondName);
            }
        }

        return true;
    }

    private void handleTeleport(Player toTp, String destName)
    {
        String toTpName = toTp.getName();
        String autoCompletedDestName = attemptAutoComplete(toTp, destName);

        if(autoCompletedDestName == null) return;

        Player destination = Bukkit.getPlayer(autoCompletedDestName);

        if(destination != null)
        {
            plugin.getTeleportListener().teleport(toTp, destination,
                PlayerTpEvent.TeleportType.NORMAL_TP);
            return;
        }

        deltaRedisApi.findPlayer(destName, cachedPlayer ->
        {
            Player player = Bukkit.getPlayer(toTpName);

            if(player == null) return;

            if(cachedPlayer == null)
            {
                CommandMessageUtil.playerOffline(player, destName);
                return;
            }

            String destServer = cachedPlayer.getServer();

            // Format: TpSender/\CurrentServer
            deltaRedisApi.publish(destServer, DeltaEssentialsChannels.TP,
                toTpName, destName);

            plugin.sendToServer(player, destServer);
        });
    }

    private String attemptAutoComplete(CommandSender sender, String partial)
    {
        List<String> partialMatches = deltaRedisApi.matchStartOfPlayerName(partial);

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

        sender.sendMessage(Prefixes.FAILURE + "Multiple online players match " +
            Prefixes.input(partial));

        return null;
    }
}

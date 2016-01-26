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
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
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
public class CommandTp implements TabExecutor, Shutdownable, Registerable
{
    private static final String TOO_MANY_MATCHES = "!TOO_MANY_MATCHES!";

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

        String destName = args[0];

        if(args.length == 1)
        {
            if(!(sender instanceof Player))
            {
                sender.sendMessage(Prefixes.FAILURE + "Only players can teleport to others.");
            }
            else if(!sender.hasPermission("DeltaEss.Tp"))
            {
                sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                    Prefixes.input("DeltaEss.Tp") + " permission.");
            }
            else
            {
                Player player = (Player) sender;
                handleTeleport(player, destName);
            }
        }
        else
        {
            Player player = Bukkit.getPlayer(destName);

            if(player == null)
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                    " is not online");
            }
            else if(!sender.hasPermission("DeltaEss.TpOther"))
            {
                sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                    Prefixes.input("DeltaEss.TpOther") + " permission.");
            }
            else
            {
                handleTeleport(player, destName);
            }
        }

        return true;
    }

    private void handleTeleport(Player toTp, String destName)
    {
        String toTpName = toTp.getName();
        String autoCompletedDestName = attemptAutoComplete(destName);

        if(autoCompletedDestName.equals(TOO_MANY_MATCHES))
        {
            toTp.sendMessage(Prefixes.FAILURE + "Too many online players match " +
                Prefixes.input(destName));
            return;
        }

        Player destination = Bukkit.getPlayer(autoCompletedDestName);

        if(destination != null)
        {
            plugin.getTeleportListener().teleport(toTp, destination);
        }
        else
        {
            deltaRedisApi.findPlayer(destName, cachedPlayer ->
            {
                Player player = Bukkit.getPlayer(toTpName);

                if(player == null) { return; }

                if(cachedPlayer != null)
                {
                    // Format: TpSender/\CurrentServer
                    String destServer = cachedPlayer.getServer();

                    deltaRedisApi.publish(destServer, DeltaEssentialsChannels.TP,
                        toTpName, destName);

                    plugin.sendToServer(player, destServer);
                }
                else
                {
                    player.sendMessage(Prefixes.FAILURE + Prefixes.input(destName) +
                        " is not online");
                }
            });
        }
    }

    private String attemptAutoComplete(String partial)
    {
        List<String> partialMatches = deltaRedisApi.matchStartOfPlayerName(partial);

        if(!partialMatches.contains(partial.toLowerCase()))
        {
            if(partialMatches.size() == 1)
            {
                return partialMatches.get(0);
            }
            else if(partialMatches.size() > 1)
            {
                return TOO_MANY_MATCHES;
            }
        }
        return partial;
    }
}

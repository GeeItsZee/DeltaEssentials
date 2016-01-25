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
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTp extends DeltaEssentialsCommand
{
    private DeltaRedisApi deltaRedisApi;

    public CommandTp(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super("tp", null, plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void shutdown()
    {
        this.deltaRedisApi = null;
        super.shutdown();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfPlayerName(lastArg);
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/tp <player>");
            return;
        }

        Settings settings = plugin.getSettings();
        String destName = args[0];

        if(args.length == 1 && sender.hasPermission("DeltaEss.Tp"))
        {
            if(sender instanceof Player)
            {
                Player player = (Player) sender;
                teleport(player, destName);
            }
            else
            {
                sender.sendMessage(Prefixes.FAILURE + "Only players can teleport to others.");
            }
        }
        else if(args.length >= 2 && sender.hasPermission("DeltaEss.TpOther"))
        {
            Player player = Bukkit.getPlayer(destName);
            if(player != null && player.isOnline())
            {
                teleport(player, destName);
            }
            else
            {
                String playerNotOnline = settings.format("PlayerNotOnline", destName);
                sender.sendMessage(playerNotOnline);
            }
        }
        else
        {
            String noPermission = settings.format("NoPermission", "DeltaEss.Tp or DeltaEss.TpOther");
            sender.sendMessage(noPermission);
        }
    }

    private void teleport(Player toTp, String destName)
    {
        String autoCompletedDestName = attemptAutoComplete(toTp, destName);
        if(autoCompletedDestName != null)
        {
            Player destination = Bukkit.getPlayer(autoCompletedDestName);
            if(destination != null && destination.isOnline())
            {
                plugin.getTeleportListener().teleport(toTp, destination);
            }
            else
            {
                handleDiffServerTeleport(toTp.getName(), autoCompletedDestName);
            }
        }
    }

    private void handleDiffServerTeleport(String senderName, String destName)
    {
        Settings settings = plugin.getSettings();

        deltaRedisApi.findPlayer(destName, cachedPlayer ->
        {
            Player toTp = Bukkit.getPlayer(senderName);
            if(toTp == null || !toTp.isOnline()) { return; }

            if(cachedPlayer != null)
            {
                // Format: TpSender/\CurrentServer
                String destServer = cachedPlayer.getServer();

                deltaRedisApi.publish(destServer, DeltaEssentialsChannels.TP,
                    senderName, destName);
                plugin.sendToServer(toTp, destServer);
            }
            else
            {
                String playerNotOnline = settings.format("PlayerNotOnline", destName);
                toTp.sendMessage(playerNotOnline);
            }
        });
    }

    private String attemptAutoComplete(CommandSender sender, String partial)
    {
        Settings settings = plugin.getSettings();
        List<String> partialMatches = deltaRedisApi.matchStartOfPlayerName(partial);

        if(!partialMatches.contains(partial.toLowerCase()))
        {
            if(partialMatches.size() == 1)
            {
                return partialMatches.get(0);
            }
            else if(partialMatches.size() > 1)
            {
                String tooManyAutoCompleteMatches = settings.format(
                    "TooManyAutoCompleteMatches", partial);

                sender.sendMessage(tooManyAutoCompleteMatches);
                return null;
            }
        }
        return partial;
    }
}

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
import com.gmail.tracebachi.DeltaEssentials.Storage.TeleportRequest;
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
public class CommandTpHere extends DeltaEssentialsCommand
{
    private DeltaRedisApi deltaRedisApi;

    public CommandTpHere(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super("tphere", "DeltaEss.TpOther", plugin);
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
            sender.sendMessage(Prefixes.INFO + "/tphere <player>");
            return;
        }

        if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can use /tphere.");
            return;
        }

        Player sendingPlayer = (Player) sender;
        String nameToTp = args[0];
        Player playerToTp = Bukkit.getPlayer(nameToTp);

        if(playerToTp != null)
        {
            plugin.getTeleportListener().teleport(playerToTp, sendingPlayer);
        }
        else
        {
            handleDiffServerTeleport(nameToTp, sender.getName());
        }
    }

    private void handleDiffServerTeleport(String nameToTp, String senderName)
    {
        deltaRedisApi.findPlayer(nameToTp, cachedPlayer ->
        {
            Player sender = Bukkit.getPlayer(senderName);

            if(sender == null) { return; }

            if(cachedPlayer != null)
            {
                // Format: Receiver/\Sender/\CurrentServer
                String destServer = cachedPlayer.getServer();
                String currentServer = deltaRedisApi.getServerName();
                TeleportRequest request = new TeleportRequest(senderName, currentServer);

                deltaRedisApi.publish(destServer, DeltaEssentialsChannels.TP_HERE,
                    nameToTp, senderName, currentServer);
                plugin.getTeleportListener().getRequestMap().put(nameToTp, request);
            }
            else
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(nameToTp) +
                    " is not online.");
            }
        });
    }
}

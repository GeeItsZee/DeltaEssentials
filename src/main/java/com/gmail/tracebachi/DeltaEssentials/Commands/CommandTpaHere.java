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
public class CommandTpaHere implements TabExecutor, Registerable, Shutdownable
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentials plugin;

    public CommandTpaHere(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("tpahere").setExecutor(this);
        plugin.getCommand("tpahere").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("tpahere").setExecutor(null);
        plugin.getCommand("tpahere").setTabCompleter(null);
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
            sender.sendMessage(Prefixes.INFO + "/tpahere <player>");
            return true;
        }

        if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can use /tpahere.");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Tpa.Send"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaEss.Tpa.Send") + " permission.");
            return true;
        }

        String senderName = sender.getName();
        String receiverName = args[0];
        Player receiver = Bukkit.getPlayer(receiverName);

        if(receiver != null)
        {
            String currentServer = deltaRedisApi.getServerName();
            TeleportRequest request = new TeleportRequest(senderName, currentServer);

            plugin.getTeleportListener().getRequestMap().put(receiverName, request);

            receiver.sendMessage(Prefixes.INFO + Prefixes.input(senderName) +
                " sent you a TPA request. Use /tpaccept within 30 seconds to accept.");

            sender.sendMessage(Prefixes.INFO + Prefixes.input(receiverName) +
                " was sent a TPA request.");
        }
        else
        {
            deltaRedisApi.findPlayer(receiverName, cachedPlayer ->
            {
                Player senderPlayer = Bukkit.getPlayer(senderName);

                if(senderPlayer == null) { return; }

                if(cachedPlayer != null)
                {
                    // Format: Receiver/\Sender/\CurrentServer
                    String destServer = cachedPlayer.getServer();
                    String currentServer = deltaRedisApi.getServerName();

                    deltaRedisApi.publish(destServer, DeltaEssentialsChannels.TPA_HERE,
                        receiverName, senderName, currentServer);

                    TeleportRequest request = new TeleportRequest(senderName, currentServer);
                    plugin.getTeleportListener().getRequestMap().put(receiverName, request);

                    senderPlayer.sendMessage(Prefixes.INFO + Prefixes.input(receiverName) +
                        " was sent a TPA request.");
                }
                else
                {
                    sender.sendMessage(Prefixes.FAILURE + Prefixes.input(receiverName) +
                        " is not online");
                }
            });
        }

        return true;
    }
}

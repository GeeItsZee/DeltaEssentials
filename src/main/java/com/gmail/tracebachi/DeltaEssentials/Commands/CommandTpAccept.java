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
import com.gmail.tracebachi.DeltaEssentials.Settings;
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
public class CommandTpAccept extends DeltaEssentialsCommand
{
    private DeltaRedisApi deltaRedisApi;

    public CommandTpAccept(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super("tpaccept", "DeltaEss.Tpa.Accept", plugin);
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
        if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can accept teleports.");
            return;
        }

        Settings settings = plugin.getSettings();
        TeleportRequest request = plugin.getTeleportListener().getRequestMap()
            .get(sender.getName());

        if(request == null)
        {
            String noTpaRequest = settings.format("NoTpaRequest");
            sender.sendMessage(noTpaRequest);
            return;
        }

        Player player = (Player) sender;

        if(request.getDestServer().equals(deltaRedisApi.getServerName()))
        {
            Player destPlayer = Bukkit.getPlayer(request.getSender());

            if(destPlayer != null)
            {
                plugin.getTeleportListener().teleport(player, destPlayer);
            }
            else
            {
                String playerNotOnline = settings.format("PlayerNotOnline", request.getSender());
                sender.sendMessage(playerNotOnline);
            }
        }
        else
        {
            plugin.sendToServer(player, request.getDestServer());
        }
    }
}

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
package com.yahoo.tracebachi.DeltaEssentials.Commands;

import com.yahoo.tracebachi.DeltaEssentials.CallbackUtil;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/4/15.
 */
public class KickCommand implements TabExecutor
{
    public static final String KICK_CHANNEL = "DE-Kick";

    private DeltaRedisApi deltaRedisApi;

    public KickCommand(DeltaRedisApi deltaRedisApi)
    {
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args)
    {
        List<String> result = new ArrayList<>();

        if(args.length != 0)
        {
            String lastArg = args[args.length - 1].toLowerCase();
            for(String name : deltaRedisApi.getCachedPlayers())
            {
                if(name.startsWith(lastArg))
                {
                    result.add(name);
                }
            }
        }
        return result;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaEss.Kick"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use that command.");
            return true;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/kick <player>");
            return true;
        }

        String nameToKick = args[0];
        String senderName = sender.getName();
        String reason = getKickMessage(args);
        Player playerToKick = Bukkit.getPlayer(nameToKick);

        if(playerToKick != null && playerToKick.isOnline())
        {
            playerToKick.kickPlayer(reason + "\n\n" + ChatColor.GOLD + senderName);
            announceKick(senderName, playerToKick.getName(), reason);
        }
        else
        {
            deltaRedisApi.findPlayer(nameToKick, cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    deltaRedisApi.publish(cachedPlayer.getServer(), KICK_CHANNEL,
                        senderName + "/\\" + nameToKick + "/\\" + reason);
                    announceKick(senderName, nameToKick, reason);
                }
                else
                {
                    CallbackUtil.sendMessage(senderName,
                        Prefixes.FAILURE + "Player not found.");
                }
            });
        }
        return true;
    }

    private String getKickMessage(String[] args)
    {
        if(args.length > 1)
        {
            String joined = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            return ChatColor.translateAlternateColorCodes('&', joined);
        }
        else
        {
            return "Kicked from server!";
        }
    }

    private void announceKick(String kicker, String playerKicked, String reason)
    {
        String announcement =
            ChatColor.GOLD + kicker + ChatColor.WHITE + " kicked " +
            ChatColor.GOLD + playerKicked + ChatColor.WHITE + " for " +
            ChatColor.GOLD + reason;

        for(Player onlinePlayer : Bukkit.getOnlinePlayers())
        {
            onlinePlayer.sendMessage(announcement);
        }
    }
}

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
package com.yahoo.tracebachi.DeltaEssentials;

import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/4/15.
 */
public class KickCommand implements CommandExecutor
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
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaEss.Kick"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use that command.");
            return true;
        }

        if(args.length == 0)
        {
            sender.sendMessage(Prefixes.INFO + "/kick name ");
            return true;
        }

        if(args.length >= 1)
        {
            final String targetName = args[0].toLowerCase();
            final String senderName = sender.getName().toLowerCase();
            Player player = Bukkit.getPlayer(targetName);
            String message = "Kicked from server!";

            if(args.length > 1)
            {
                message = ChatColor.translateAlternateColorCodes('&',
                    String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            }

            if(player != null && player.isOnline())
            {
                player.kickPlayer(message + "\n\n - " + ChatColor.GOLD + senderName);

                for(Player onlinePlayer : Bukkit.getOnlinePlayers())
                {
                    onlinePlayer.sendMessage(
                        ChatColor.GOLD + senderName +
                        ChatColor.WHITE + " kicked " +
                        ChatColor.GOLD + targetName +
                        ChatColor.WHITE + " for " +
                        ChatColor.GOLD + message);
                }
            }
            else
            {
                String finalMessage = message;
                deltaRedisApi.findPlayer(targetName, cachedPlayer -> {

                    if(cachedPlayer != null)
                    {
                        deltaRedisApi.publish(cachedPlayer.getServer(),
                            KICK_CHANNEL, targetName + "/\\" + senderName + "/\\" + finalMessage);

                        for(Player onlinePlayer : Bukkit.getOnlinePlayers())
                        {
                            onlinePlayer.sendMessage(
                                ChatColor.GOLD + senderName +
                                ChatColor.WHITE + " kicked " +
                                ChatColor.GOLD + targetName +
                                ChatColor.WHITE + " for " +
                                ChatColor.GOLD + finalMessage);
                        }
                    }
                    else
                    {
                        if(senderName.equals("console"))
                        {
                            Bukkit.getConsoleSender().sendMessage(Prefixes.FAILURE + "Player not found.");
                        }
                        else
                        {
                            Player originalSender = Bukkit.getPlayer(senderName);
                            if(originalSender != null && originalSender.isOnline())
                            {
                                originalSender.sendMessage(Prefixes.FAILURE + "Player not found.");
                            }
                        }
                    }
                });
            }
        }

        return true;
    }
}

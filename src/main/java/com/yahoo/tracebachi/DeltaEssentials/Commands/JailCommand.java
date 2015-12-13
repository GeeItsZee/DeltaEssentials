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

import com.earth2me.essentials.utils.DateUtil;
import com.yahoo.tracebachi.DeltaEssentials.CallbackUtil;
import com.yahoo.tracebachi.DeltaEssentials.DeltaEssentialsPlugin;
import com.yahoo.tracebachi.DeltaEssentials.Prefixes;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/4/15.
 */
public class JailCommand implements CommandExecutor
{
    public static final String JAIL_CHANNEL = "DE-Jail";

    private final String jailServer;
    private Set<String> validJails;
    private DeltaEssentialsPlugin plugin;
    private DeltaRedisApi deltaRedisApi;

    public JailCommand(String jailServer, List<String> validJails, DeltaEssentialsPlugin plugin, DeltaRedisApi deltaRedisApi)
    {
        this.jailServer = jailServer;
        this.validJails = new HashSet<>(validJails);
        this.plugin = plugin;
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.validJails = null;
        this.plugin = null;
        this.deltaRedisApi = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaEss.Jail"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use that command.");
            return true;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/jail <player> <jailname> [datediff]");
            return true;
        }

        String senderName = sender.getName();
        if(deltaRedisApi.getServerName().equals(jailServer))
        {
            String joined = String.join(" ", Arrays.asList(args));
            Bukkit.dispatchCommand(sender, "essentials:jail " + joined);

            Player playerToJail = Bukkit.getPlayer(args[0]);
            if(playerToJail == null)
            {
                deltaRedisApi.findPlayer(args[0], cachedPlayer ->
                {
                    if(cachedPlayer != null)
                    {
                        deltaRedisApi.publish(cachedPlayer.getServer(), MoveToCommand.MOVE_CHANNEL,
                            senderName + "/\\" + args[0] + "/\\" + jailServer);
                    }
                    else
                    {
                        CallbackUtil.sendMessage(senderName, Prefixes.FAILURE + "Player not found.");
                    }
                });
            }

            return true;
        }

        String jailName = "";
        if(args.length >= 2)
        {
            if(!validJails.contains(args[1]))
            {
                sender.sendMessage(Prefixes.FAILURE + args[1] + " (jail) does not exist.");
                return true;
            }
            else
            {
                jailName = args[1];
            }
        }

        String timeDiff = "";
        if(args.length >= 3)
        {
            if(!validateTimeDiff(args[2]))
            {
                sender.sendMessage(Prefixes.FAILURE + "That is not a valid datediff.");
                return true;
            }
            else
            {
                timeDiff = args[2];
            }
        }

        Player playerToJail = Bukkit.getPlayer(args[0]);
        if(playerToJail != null && playerToJail.isOnline())
        {
            // Alert the jail server of the player
            deltaRedisApi.publish(jailServer, JAIL_CHANNEL,
                senderName + "/\\essentials:jail " +
                args[0] + " " + jailName + " " + timeDiff);

            // Since player is on the same server, send to jail server
            plugin.sendToServer(playerToJail, jailServer);
        }
        else
        {
            // Alert the jail server of the player
            deltaRedisApi.publish(jailServer, JAIL_CHANNEL,
                senderName + "/\\essentials:jail " +
                args[0] + " " + jailName + " " + timeDiff);

            // Find player and move them to the jail server
            deltaRedisApi.findPlayer(args[0], cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    if(!cachedPlayer.getServer().equals(jailServer))
                    {
                        deltaRedisApi.publish(cachedPlayer.getServer(), MoveToCommand.MOVE_CHANNEL,
                            senderName + "/\\" + args[0] + "/\\" + jailServer);
                    }
                }
                else
                {
                    CallbackUtil.sendMessage(senderName, Prefixes.FAILURE + "Player not found.");
                }
            });
        }
        return true;
    }

    private boolean validateTimeDiff(String arg)
    {
        try
        {
            DateUtil.parseDateDiff(arg, true);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
}

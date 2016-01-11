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
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/4/15.
 */
public class JailCommand implements TabExecutor
{
    public static final String JAIL_CHANNEL = "DE-Jail";

    private final String jailServer;
    private final Set<String> validJails;
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentialsPlugin plugin;

    public JailCommand(DeltaRedisApi deltaRedisApi, DeltaEssentialsPlugin plugin)
    {
        this.jailServer = plugin.getConfig().getString("JailServer");
        this.validJails = new HashSet<>(plugin.getConfig().getStringList("ValidJails"));
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
        this.plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args)
    {
        if(args.length != 0)
        {
            String lastArg = args[args.length - 1];
            return deltaRedisApi.matchStartOfName(lastArg);
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        if(!commandSender.hasPermission("DeltaEss.Jail"))
        {
            commandSender.sendMessage(Prefixes.FAILURE + "You do not have permission to use that command.");
            return true;
        }

        if(args.length < 2)
        {
            commandSender.sendMessage(Prefixes.INFO + "/jail <player> <jail name> [date diff]");
            return true;
        }

        String sender = commandSender.getName();
        String target = args[0];
        String jailName = args[1];
        String dateDiff = "";
        Player playerToJail = Bukkit.getPlayer(target);

        // Check if jail name is valid
        if(!validJails.contains(args[1]))
        {
            commandSender.sendMessage(Prefixes.FAILURE + "The jail " +
                Prefixes.input(args[1]) + " does not exist.");
            return true;
        }

        // Check if the date difference is valid (if there is a date difference)
        if(args.length >= 3)
        {
            dateDiff = args[2];
            if(!isValidDateDifference(dateDiff))
            {
                commandSender.sendMessage(Prefixes.FAILURE + Prefixes.input(args[2]) +
                    " is not a valid date difference.");
                return true;
            }
        }

        // Check if sender is in jail server
        if(deltaRedisApi.getServerName().equals(jailServer))
        {
            // If the player is not in the current server
            if(playerToJail == null)
            {
                deltaRedisApi.findPlayer(target, cachedPlayer ->
                {
                    if(cachedPlayer != null)
                    {
                        deltaRedisApi.publish(cachedPlayer.getServer(), MoveToCommand.MOVE_CHANNEL,
                            sender + "/\\" + target + "/\\" + jailServer);
                    }
                    else
                    {
                        CallbackUtil.sendMessage(sender, Prefixes.FAILURE + "Player not found.");
                    }
                });
            }

            // Run the command on the current server
            String joined = String.join(" ", Arrays.asList(args));
            Bukkit.dispatchCommand(commandSender, "essentials:jail " + joined);
            return true;
        }
        else
        {
            // If the player is on the current server
            if(playerToJail != null && playerToJail.isOnline())
            {
                plugin.sendToServer(playerToJail, jailServer);
            }
            else
            {
                deltaRedisApi.findPlayer(target, cachedPlayer ->
                {
                    if(cachedPlayer != null)
                    {
                        deltaRedisApi.publish(cachedPlayer.getServer(), MoveToCommand.MOVE_CHANNEL,
                            sender + "/\\" + target + "/\\" + jailServer);
                    }
                    else
                    {
                        CallbackUtil.sendMessage(sender, Prefixes.FAILURE + "Player not found.");
                    }
                });
            }

            // Alert the jail server of the player
            deltaRedisApi.publish(jailServer, JAIL_CHANNEL, sender +
                "/\\essentials:jail " + args[0] + " " + jailName + " " + dateDiff);
        }
        return true;
    }

    private boolean isValidDateDifference(String arg)
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

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

import com.earth2me.essentials.utils.DateUtil;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsChannels;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent.DELTA_PATTERN;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandJail implements TabExecutor, Registerable, Shutdownable, Listener
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentials plugin;

    public CommandJail(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("jail").setExecutor(this);
        plugin.getCommand("jail").setTabCompleter(this);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("jail").setExecutor(null);
        plugin.getCommand("jail").setTabCompleter(null);

        HandlerList.unregisterAll(this);
    }

    @Override
    public void shutdown()
    {
        unregister();
        deltaRedisApi = null;
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args)
    {
        String lastArg = args[args.length - 1].toLowerCase();
        return deltaRedisApi.matchStartOfServerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/jail <player> <jail name> [date diff]");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Jail"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaEss.Jail") + " permission.");
            return true;
        }

        Settings settings = plugin.getSettings();
        String jailServer = settings.getJailServer();
        String senderName = sender.getName();
        String toJailName = args[0];
        String jailName = "";
        String dateDiff = "";
        Player toJail = Bukkit.getPlayer(toJailName);

        if(args.length >= 2)
        {
            jailName = args[1];

            if(!settings.isValidJail(jailName))
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(jailName) +
                    " is not a valid jail.");
                return true;
            }
        }

        if(args.length >= 3)
        {
            dateDiff = args[2];

            if(!isValidDateDifference(dateDiff))
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(dateDiff) +
                    " is not a valid time difference.");
                return true;
            }
        }

        if(deltaRedisApi.getServerName().equals(jailServer))
        {
            String joined = String.join(" ", Arrays.asList(args));

            plugin.info(sender + " ran /essentials:jail " + joined);
            Bukkit.dispatchCommand(sender, "essentials:jail " + joined);

            if(toJail == null)
            {
                moveToJailServer(toJailName, senderName, jailServer);
            }
        }
        else
        {
            deltaRedisApi.publish(jailServer, DeltaEssentialsChannels.JAIL,
                senderName, toJailName + " " + jailName + " " + dateDiff);

            if(toJail != null)
            {
                plugin.sendToServer(toJail, jailServer);
            }
            else
            {
                moveToJailServer(toJailName, senderName, jailServer);
            }
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onRedisMessageEvent(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.JAIL))
        {
            String[] split = DELTA_PATTERN.split(event.getMessage(), 2);
            String sender = split[0];
            String command = split[1];

            plugin.info(sender + " ran /essentials:jail " + command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "essentials:jail " + command);
        }
    }

    private void moveToJailServer(String toJailName, String senderName, String jailServer)
    {
        deltaRedisApi.findPlayer(toJailName, cachedPlayer ->
        {
            if(cachedPlayer != null)
            {
                deltaRedisApi.publish(cachedPlayer.getServer(),
                    DeltaEssentialsChannels.MOVE,
                    senderName, toJailName, jailServer);
            }
            else
            {
                String offlineMessage = Prefixes.FAILURE +
                    Prefixes.input(toJailName) + " is not online";

                MessageUtil.sendMessage(senderName, offlineMessage);
            }
        });
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

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
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
public class CommandKick implements TabExecutor, Registerable, Shutdownable, Listener
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentials plugin;

    public CommandKick(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("kick").setExecutor(this);
        plugin.getCommand("kick").setTabCompleter(this);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("kick").setExecutor(null);
        plugin.getCommand("kick").setTabCompleter(null);

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
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfPlayerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/kick <player> [message]");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Tell.Use"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaEss.Kick") + " permission.");
            return true;
        }

        Settings settings = plugin.getSettings();
        String senderName = sender.getName();
        String toKickName = args[0];
        Player toKick = Bukkit.getPlayer(toKickName);
        String reason;

        if(args.length > 1)
        {
            String joined = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            reason = ChatColor.translateAlternateColorCodes('&', joined);
        }
        else
        {
            reason = settings.format("DefaultKickReason");
        }

        if(toKick != null)
        {
            String kickPlayer = settings.format("KickPlayer", senderName, reason);
            toKick.kickPlayer(kickPlayer);

            announceKick(settings, toKickName, senderName, reason);
        }
        else
        {
            deltaRedisApi.findPlayer(toKickName, cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    deltaRedisApi.publish(Servers.SPIGOT,
                        DeltaEssentialsChannels.KICK,
                        senderName, toKickName, reason);

                    announceKick(settings, toKickName, senderName, reason);
                }
                else
                {
                    String offlineMessage = Prefixes.FAILURE +
                        Prefixes.input(toKickName) + " is not online";

                    MessageUtil.sendMessage(senderName, offlineMessage);
                }
            });
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.KICK))
        {
            Settings settings = plugin.getSettings();
            String[] split = DELTA_PATTERN.split(event.getMessage(), 3);
            String senderName = split[0];
            String toKickName = split[1];
            String reason = split[2];
            Player toKick = Bukkit.getPlayer(toKickName);

            if(toKick != null)
            {
                String kickPlayer = settings.format("KickPlayer", senderName, reason);
                toKick.kickPlayer(kickPlayer);
            }

            announceKick(settings, toKickName, senderName, reason);
        }
    }

    private void announceKick(Settings settings, String toKickName, String senderName, String reason)
    {
        String kickAnnounce = settings.format("KickAnnounce", senderName, toKickName, reason);

        for(Player onlinePlayer : Bukkit.getOnlinePlayers())
        {
            onlinePlayer.sendMessage(kickAnnounce);
        }
    }
}

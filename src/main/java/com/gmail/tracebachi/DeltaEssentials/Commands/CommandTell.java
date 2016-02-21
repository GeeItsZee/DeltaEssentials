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
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTellEvent;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayer;
import com.gmail.tracebachi.DeltaEssentials.Utils.CommandMessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
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
import java.util.Map;

import static com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent.DELTA_PATTERN;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTell implements TabExecutor, Registerable, Shutdownable, Listener
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaEssentials plugin;

    public CommandTell(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("tell").setExecutor(this);
        plugin.getCommand("tell").setTabCompleter(this);

        plugin.getCommand("reply").setExecutor(this);
        plugin.getCommand("reply").setTabCompleter(this);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("tell").setExecutor(null);
        plugin.getCommand("tell").setTabCompleter(null);

        plugin.getCommand("reply").setExecutor(null);
        plugin.getCommand("reply").setTabCompleter(null);

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
        String senderName = sender.getName();
        String commandName = command.getName();
        String receiverName;
        String message;

        if(commandName.equalsIgnoreCase("reply") && args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/reply <message>");
            return true;
        }
        else if(commandName.equalsIgnoreCase("tell") && args.length < 2)
        {
            sender.sendMessage(Prefixes.INFO + "/tell <name> <message>");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Tell.Use"))
        {
            CommandMessageUtil.noPermission(sender, "DeltaEss.Tell.Use");
            return true;
        }

        DeltaEssPlayer dePlayer = plugin.getPlayerMap().get(senderName);

        if(dePlayer == null)
        {
            sender.sendMessage(Prefixes.FAILURE + "Player data has not been loaded.");
            return true;
        }

        if(commandName.equalsIgnoreCase("reply"))
        {
            receiverName = dePlayer.getLastReplyTarget();

            if(receiverName.equals(""))
            {
                sender.sendMessage(Prefixes.FAILURE + "You do not have anyone to reply to.");
                return true;
            }

            message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        }
        else
        {
            if(args[0].equalsIgnoreCase("console"))
            {
                receiverName = "console";
            }
            else
            {
                receiverName = attemptAutoComplete(sender, args[0]);

                if(receiverName == null) return true;
            }

            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        if(sender.hasPermission("DeltaEss.Tell.Color"))
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        if(receiverName.equals("console"))
        {
            sendMessage(
                senderName, sender,
                receiverName, Bukkit.getConsoleSender(),
                message);
            return true;
        }

        Player receiver = Bukkit.getPlayer(receiverName);

        if(receiver != null)
        {
            sendMessage(
                senderName, sender,
                receiverName, receiver,
                message);
            return true;
        }

        boolean result = sendMessage(
            senderName, sender,
            receiverName, null,
            message);

        if(result)
        {
            String finalMessage = message;

            deltaRedisApi.findPlayer(receiverName, cachedPlayer ->
            {
                if(cachedPlayer == null)
                {
                    CommandMessageUtil.playerOffline(senderName, receiverName);
                    return;
                }

                String destination = cachedPlayer.getServer();

                // Format: SenderName/\ReceiverName/\Message
                deltaRedisApi.publish(destination,
                    DeltaEssentialsChannels.TELL,
                    senderName, receiverName, finalMessage);
            });
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.TELL))
        {
            String[] split = DELTA_PATTERN.split(event.getMessage(), 3);
            String senderName = split[0];
            String receiverName = split[1];
            String message = split[2];
            Player receiver = Bukkit.getPlayer(receiverName);

            if(receiver == null)
            {
                CommandMessageUtil.playerOffline(senderName, receiverName);
                return;
            }

            sendMessage(
                senderName, null,
                receiverName, receiver,
                message);
        }
    }

    private boolean sendMessage(String senderName, CommandSender sender,
        String receiverName, CommandSender receiver, String message)
    {
        Settings settings = plugin.getSettings();
        CaseInsensitiveHashMap<DeltaEssPlayer> playerMap = plugin.getPlayerMap();
        PlayerTellEvent event = new PlayerTellEvent(senderName, sender,
            receiverName, receiver, message);

        Bukkit.getPluginManager().callEvent(event);

        if(!event.isCancelled())
        {
            String logFormat = settings.format("TellLog", senderName, receiverName, message);
            Bukkit.getLogger().info(logFormat);

            String spyFormat = settings.format("TellSpy", senderName, receiverName, message);
            sendToAllSocialSpies(playerMap, spyFormat);

            if(sender != null)
            {
                String senderFormat = settings.format("TellSender", receiverName, message);
                sender.sendMessage(senderFormat);

                DeltaEssPlayer dePlayer = playerMap.get(senderName);
                dePlayer.setLastReplyTarget(receiverName);
            }

            if(receiver != null)
            {
                String receiverFormat = settings.format("TellReceiver", senderName, message);
                receiver.sendMessage(receiverFormat);

                DeltaEssPlayer dePlayer = playerMap.get(receiverName);
                dePlayer.setLastReplyTarget(senderName);
            }

            return true;
        }

        return false;
    }

    private void sendToAllSocialSpies(CaseInsensitiveHashMap<DeltaEssPlayer> playerMap,
        String spyFormat)
    {
        for(Map.Entry<String, DeltaEssPlayer> entry : playerMap.entrySet())
        {
            if(entry.getValue().isSocialSpyEnabled())
            {
                Player player = Bukkit.getPlayer(entry.getKey());

                if(player != null)
                {
                    player.sendMessage(spyFormat);
                }
            }
        }
    }

    private String attemptAutoComplete(CommandSender sender, String partial)
    {
        List<String> partialMatches = deltaRedisApi.matchStartOfPlayerName(partial);

        if(partialMatches.contains(partial.toLowerCase()))
        {
            return partial;
        }

        if(partialMatches.size() == 0)
        {
            return partial;
        }

        if(partialMatches.size() == 1)
        {
            return partialMatches.get(0);
        }

        sender.sendMessage(Prefixes.FAILURE + "Multiple online players match " +
            Prefixes.input(partial));

        return null;
    }
}

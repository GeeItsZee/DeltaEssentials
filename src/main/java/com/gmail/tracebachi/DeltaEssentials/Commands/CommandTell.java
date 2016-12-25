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
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTellReceiveEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTellSendEvent;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.SocialSpyLevel;
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Callback;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.Events.DeltaRedisMessageEvent;
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

import java.util.*;
import java.util.regex.Pattern;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTell implements TabExecutor, Registerable, Shutdownable, Listener
{
    private static final String TELL_USE_PERM = "DeltaEss.Tell.Use";
    private static final String TELL_COLOR_PERM = "DeltaEss.Tell.Color";
    private static final String TELL_IGNORE_VANISH_PERM = "DeltaEss.Tell.IgnoreVanish";
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    private DeltaEssentials plugin;
    private String consoleReplyingTo = "";

    public CommandTell(DeltaEssentials plugin)
    {
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
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command,
                                      String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return DeltaRedisApi.instance().matchStartOfPlayerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission(TELL_USE_PERM))
        {
            sender.sendMessage(formatNoPerm(TELL_USE_PERM));
            return true;
        }

        String commandName = command.getName();
        if(commandName.equalsIgnoreCase("reply"))
        {
            if(args.length < 1)
            {
                sender.sendMessage(formatUsage("/reply <message>"));
            }
            else
            {
                handleReplyCommand(sender, args);
            }
        }
        else if(commandName.equalsIgnoreCase("tell"))
        {
            if(args.length < 2)
            {
                sender.sendMessage(formatUsage("/tell <name> <message>"));
            }
            else
            {
                handleTellCommand(sender, args);
            }
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.SOCIAL_SPY))
        {
            List<String> messageParts = event.getMessageParts();
            String senderName = messageParts.get(0);
            String receiverName = messageParts.get(1);
            String message = messageParts.get(2);

            sendToSocialSpies(senderName, receiverName, message);
        }
        else if(event.getChannel().equals(DeltaEssentialsChannels.TELL))
        {
            List<String> messageParts = event.getMessageParts();
            String senderName = messageParts.get(0);
            String receiverName = messageParts.get(1);
            String message = messageParts.get(2);

            Set<String> senderPerms = new HashSet<>();
            Collections.addAll(senderPerms, COMMA_PATTERN.split(messageParts.get(3)));

            handleReceivingMessage(senderName, receiverName, message, senderPerms);
        }
    }

    private void handleReplyCommand(CommandSender sender, String[] args)
    {
        String senderName = sender.getName();
        String receiverName;

        if(senderName.equalsIgnoreCase("console"))
        {
            receiverName = consoleReplyingTo;
            if(receiverName.isEmpty())
            {
                sender.sendMessage(format("DeltaEss.NoReplyTarget"));
                return;
            }
        }
        else
        {
            Map<String, DeltaEssPlayerData> playerMap = plugin.getPlayerDataMap();
            DeltaEssPlayerData senderData = playerMap.get(senderName);

            if(senderData == null)
            {
                sender.sendMessage(format("DeltaEss.PlayerDataNotLoaded"));
                return;
            }

            receiverName = senderData.getReplyingTo();
            if(receiverName.isEmpty())
            {
                sender.sendMessage(format("DeltaEss.NoReplyTarget"));
                return;
            }
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

        if(sender.hasPermission(TELL_COLOR_PERM))
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        handleTellingMessage(
            senderName,
            receiverName,
            message,
            sender.hasPermission(TELL_IGNORE_VANISH_PERM));
    }

    private void handleTellCommand(CommandSender sender, String[] args)
    {
        String senderName = sender.getName();
        DeltaEssPlayerData senderData = plugin.getPlayerDataMap().get(senderName);

        if(!senderName.equalsIgnoreCase("console") && senderData == null)
        {
            sender.sendMessage(format("DeltaEss.PlayerDataNotLoaded"));
            return;
        }

        String receiverName = attemptAutoComplete(args[0]);
        if(receiverName == null)
        {
            sender.sendMessage(format("DeltaEss.TooManyAutoCompleteMatches", args[0]));
            return;
        }

        if(senderName.equalsIgnoreCase("console"))
        {
            consoleReplyingTo = receiverName;
        }
        else
        {
            senderData.setReplyingTo(receiverName);
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if(sender.hasPermission(TELL_COLOR_PERM))
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        handleTellingMessage(
            senderName,
            receiverName,
            message,
            sender.hasPermission(TELL_IGNORE_VANISH_PERM));
    }

    private void handleTellingMessage(String senderName, String receiverName, String message,
                                      boolean senderIgnoresVanishPerm)
    {
        Callback<PlayerTellSendEvent> callback = event ->
        {
            if(event.isCancelled()) { return; }

            String eventMessage = event.getMessage();
            String eventSenderPerms = String.join(",", event.getSenderPermissions());

            Bukkit.getScheduler().runTask(plugin, () ->
            {
                CommandSender eventSender = getCommandSender(senderName);
                if(eventSender == null) { return; }

                Bukkit.getLogger().info(format(
                    "DeltaEss.TellLog",
                    senderName,
                    receiverName,
                    eventMessage));

                eventSender.sendMessage(format(
                    "DeltaEss.TellSender",
                    receiverName,
                    eventMessage));

                DeltaRedisApi.instance().publish(
                    Servers.SPIGOT,
                    DeltaEssentialsChannels.SOCIAL_SPY,
                    senderName,
                    receiverName,
                    eventMessage);

                if(receiverName.equalsIgnoreCase("console") ||
                    Bukkit.getPlayerExact(receiverName) != null)
                {
                    handleReceivingMessage(
                        senderName,
                        receiverName,
                        eventMessage,
                        event.getSenderPermissions());
                    return;
                }

                DeltaRedisApi.instance().findPlayer(receiverName, cachedPlayer ->
                {
                    if(cachedPlayer == null)
                    {
                        MessageUtil.sendMessage(
                            senderName,
                            formatPlayerOffline(receiverName));
                    }
                    else
                    {
                        DeltaRedisApi.instance().publish(
                            Servers.SPIGOT,
                            DeltaEssentialsChannels.TELL,
                            senderName,
                            receiverName,
                            eventMessage,
                            eventSenderPerms);
                    }
                });
            });
        };

        PlayerTellSendEvent event = new PlayerTellSendEvent(
            senderName,
            receiverName,
            message,
            callback);

        if(senderIgnoresVanishPerm)
        {
            event.getSenderPermissions().add(TELL_IGNORE_VANISH_PERM);
        }

        Bukkit.getPluginManager().callEvent(event);
    }

    private void handleReceivingMessage(String senderName, String receiverName, String message,
                                        Set<String> senderPermissions)
    {
        Callback<PlayerTellReceiveEvent> callback = (event) ->
        {
            if(event.isCancelled()) { return; }

            CommandSender receiver = getCommandSender(receiverName);

            if(receiver == null)
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    senderName,
                    formatPlayerOffline(receiverName));
                return;
            }

            DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(receiverName);
            boolean hasIgnoreVanishPerm = event.hasSenderPermission(TELL_IGNORE_VANISH_PERM);

            if(playerData != null && playerData.isVanished() && !hasIgnoreVanishPerm)
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    senderName,
                    formatPlayerOffline(receiverName));
                return;
            }

            Bukkit.getLogger().info(format(
                "DeltaEss.TellLog",
                senderName,
                receiverName,
                message));

            receiver.sendMessage(format(
                "DeltaEss.TellReceiver",
                senderName,
                message));

            if(receiverName.equalsIgnoreCase("console"))
            {
                consoleReplyingTo = senderName;
            }
            else if(playerData != null)
            {
                playerData.setReplyingTo(senderName);
            }
        };

        PlayerTellReceiveEvent receiveEvent = new PlayerTellReceiveEvent(
            senderName,
            receiverName,
            message,
            senderPermissions,
            callback);
        Bukkit.getPluginManager().callEvent(receiveEvent);
    }

    private String attemptAutoComplete(String partial)
    {
        List<String> partialMatches = DeltaRedisApi.instance().matchStartOfPlayerName(partial);

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

        return null;
    }

    private void sendToSocialSpies(String senderName, String receiverName, String message)
    {
        String spyFormat = format("DeltaEss.TellSpy", senderName, receiverName, message);
        Player sender = Bukkit.getPlayerExact(senderName);
        Player receiver = Bukkit.getPlayerExact(receiverName);
        boolean participantInWorld = sender != null ||
            receiver != null ||
            senderName.equalsIgnoreCase("console") ||
            receiverName.equalsIgnoreCase("console");

        for(Map.Entry<String, DeltaEssPlayerData> entry : plugin.getPlayerDataMap().entrySet())
        {
            SocialSpyLevel socialSpyLevel = entry.getValue().getSocialSpyLevel();

            if((socialSpyLevel == SocialSpyLevel.WORLD && participantInWorld) ||
                socialSpyLevel == SocialSpyLevel.ALL)
            {
                Player player = Bukkit.getPlayerExact(entry.getKey());

                if(player != null)
                {
                    player.sendMessage(spyFormat);
                }
            }
        }
    }

    private CommandSender getCommandSender(String name)
    {
        return (name.equalsIgnoreCase("console")) ?
            Bukkit.getConsoleSender() :
            Bukkit.getPlayerExact(name);
    }
}

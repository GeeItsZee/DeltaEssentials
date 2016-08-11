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
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.SocialSpyLevel;
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.SplitPatterns;
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

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTell implements TabExecutor, Registerable, Shutdownable, Listener
{
    private DeltaEssentials plugin;

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
        String senderName = sender.getName();
        String commandName = command.getName();
        String receiverName;
        String message;

        if(commandName.equalsIgnoreCase("reply") && args.length < 1)
        {
            sender.sendMessage(Settings.format("ReplyUsage"));
            return true;
        }
        else if(commandName.equalsIgnoreCase("tell") && args.length < 2)
        {
            sender.sendMessage(Settings.format("TellUsage"));
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Tell.Use"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaEss.Tell.Use"));
            return true;
        }

        Map<String, DeltaEssPlayerData> playerMap = plugin.getPlayerMap();
        DeltaEssPlayerData senderData = playerMap.get(senderName);

        if(senderData == null)
        {
            sender.sendMessage(Settings.format("PlayerDataNotLoaded"));
            return true;
        }

        if(commandName.equalsIgnoreCase("reply"))
        {
            receiverName = senderData.getReplyTo();

            if(receiverName.equals(""))
            {
                sender.sendMessage(Settings.format("NoReplyTarget"));
                return true;
            }

            message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        }
        else
        {
            receiverName = attemptAutoComplete(args[0]);

            if(receiverName == null)
            {
                sender.sendMessage(Settings.format("TooManyAutoCompleteMatches", args[0]));
                return true;
            }

            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        if(sender.hasPermission("DeltaEss.Tell.Color"))
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        Player receiver = Bukkit.getPlayerExact(receiverName);
        boolean senderIgnoresVanish = sender.hasPermission("DeltaEss.Tell.IgnoreVanish");

        if(receiver != null)
        {
            DeltaEssPlayerData receiverData = playerMap.get(receiverName);

            if(receiverData != null && receiverData.isVanishEnabled() && !senderIgnoresVanish)
            {
                sender.sendMessage(Settings.format("PlayerOffline", receiverName));
                return true;
            }

            PlayerTellEvent event = new PlayerTellEvent(
                senderName,
                sender,
                receiverName,
                receiver,
                message);

            Bukkit.getPluginManager().callEvent(event);
            String finalMessage = event.getMessage();

            if(event.isCancelled()) { return true; }

            Bukkit.getLogger().info(Settings.format(
                "TellLog",
                senderName,
                receiverName,
                finalMessage));
            sender.sendMessage(Settings.format(
                "TellSender",
                receiverName,
                finalMessage));
            receiver.sendMessage(Settings.format(
                "TellReceiver",
                senderName,
                finalMessage));

            senderData.setReplyTo(receiverName);

            if(receiverData != null)
            {
                receiverData.setReplyTo(senderName);
            }

            sendToSocialSpies(senderName, receiverName, finalMessage, true);
        }
        else
        {
            PlayerTellEvent event = new PlayerTellEvent(
                senderName,
                sender,
                receiverName,
                null,
                message);

            Bukkit.getPluginManager().callEvent(event);
            String finalMessage = event.getMessage();

            if(event.isCancelled()) { return true; }

            Bukkit.getLogger().info(Settings.format(
                "TellLog",
                senderName,
                receiverName,
                finalMessage));
            sender.sendMessage(Settings.format(
                "TellSender",
                receiverName,
                finalMessage));

            senderData.setReplyTo(receiverName);

            DeltaRedisApi.instance().findPlayer(receiverName, cachedPlayer ->
            {
                if(cachedPlayer == null)
                {
                    MessageUtil.sendMessage(
                        senderName,
                        Settings.format("PlayerOffline", receiverName));
                    return;
                }

                // Format: SenderName/\ReceiverName/\Message/\IgnoreVanish
                DeltaRedisApi.instance().publish(
                    Servers.SPIGOT,
                    DeltaEssentialsChannels.TELL,
                    senderName,
                    receiverName,
                    finalMessage,
                    senderIgnoresVanish ? "1" : "0");

                sendToSocialSpies(senderName, receiverName, finalMessage, true);
            });
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.TELL) && !event.isSendingServerSelf())
        {
            String[] split = SplitPatterns.DELTA.split(event.getMessage(), 4);
            String senderName = split[0];
            String receiverName = split[1];
            String message = split[2];
            boolean senderIgnoresVanish = split[3].equals("1");

            Player receiver = Bukkit.getPlayerExact(receiverName);

            if(receiver == null)
            {
                sendToSocialSpies(senderName, receiverName, message, false);
                return;
            }

            DeltaEssPlayerData receiverData = plugin.getPlayerMap().get(receiverName);

            if(receiverData != null && receiverData.isVanishEnabled() && !senderIgnoresVanish)
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    senderName,
                    Settings.format("PlayerOffline", receiverName));
                return;
            }

            PlayerTellEvent tellEvent = new PlayerTellEvent(
                senderName,
                null,
                receiverName,
                receiver,
                message);

            Bukkit.getPluginManager().callEvent(tellEvent);
            String finalMessage = tellEvent.getMessage();

            if(tellEvent.isCancelled()) { return; }

            Bukkit.getLogger().info(Settings.format(
                "TellLog",
                senderName,
                receiverName,
                finalMessage));
            receiver.sendMessage(Settings.format(
                "TellReceiver",
                senderName,
                finalMessage));

            if(receiverData != null)
            {
                receiverData.setReplyTo(senderName);
            }

            sendToSocialSpies(senderName, receiverName, finalMessage, true);
        }
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

    private void sendToSocialSpies(String senderName, String receiverName, String message,
                                   boolean participantInWorld)
    {
        String spyFormat = Settings.format("TellSpy", senderName, receiverName, message);

        for(Map.Entry<String, DeltaEssPlayerData> entry : plugin.getPlayerMap().entrySet())
        {
            SocialSpyLevel socialSpyLevel = entry.getValue().getSocialSpyLevel();

            if(socialSpyLevel == SocialSpyLevel.ALL ||
                (socialSpyLevel == SocialSpyLevel.WORLD && participantInWorld))
            {
                Player player = Bukkit.getPlayerExact(entry.getKey());

                if(player != null)
                {
                    player.sendMessage(spyFormat);
                }
            }
        }
    }
}

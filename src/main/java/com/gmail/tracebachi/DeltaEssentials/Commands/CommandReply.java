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
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssentialsPlayer;
import com.gmail.tracebachi.DeltaEssentials.Utils.CallbackUtil;
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtils;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandReply extends DeltaEssentialsCommand
{
    private DeltaRedisApi deltaRedisApi;

    public CommandReply(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super("reply", "DeltaEss.Tell.Use", plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void shutdown()
    {
        this.deltaRedisApi = null;
        super.shutdown();
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/tell <name> <message>");
            sender.sendMessage(Prefixes.INFO + "/reply <message>");
            return;
        }

        String senderName = sender.getName();
        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(senderName);

        if(dePlayer == null)
        {
            sender.sendMessage(Prefixes.FAILURE + "Your data has not been loaded!");
            return;
        }

        String receiverName = dePlayer.getLastReplyTarget();

        if(receiverName.equals(""))
        {
            sender.sendMessage(Prefixes.FAILURE + "You have no one to reply to.");
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

        if(sender.hasPermission("DeltaEss.Tell.Color"))
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        // Check if the receiver is console
        if(receiverName.equalsIgnoreCase("console"))
        {
            if(handleConsoleTell(sender, message))
            {
                setLastReplyTargetForReceiver(senderName, receiverName);
            }
            return;
        }

        // Check if the receiver is online on the same server
        Player receiver = Bukkit.getPlayer(receiverName);
        if(receiver != null && receiver.isOnline())
        {
            if(handleSameServerTell(sender, receiver, message))
            {
                setLastReplyTargetForReceiver(senderName, receiverName);
            }
        }
        else
        {
            handleDiffServerTell(sender, receiverName, message);
        }
    }

    private boolean handleConsoleTell(CommandSender sender, String message)
    {
        String senderName = sender.getName();
        PlayerTellEvent event = plugin.sendMessageFromPlayer(
            senderName, "Console", message);

        if(!event.isCancelled())
        {
            // In case the message was modified, update it
            message = event.getMessage();

            String forReceiver = MessageUtils.formatForReceiver(senderName, message);
            Bukkit.getConsoleSender().sendMessage(forReceiver);

            String forSender = MessageUtils.formatForSender("Console", message);
            sender.sendMessage(forSender);
            return true;
        }
        else if(event.getCancelReason() != null)
        {
            sender.sendMessage(event.getCancelReason());
        }
        return false;
    }

    private boolean handleSameServerTell(CommandSender sender, Player receiver, String message)
    {
        String senderName = sender.getName();
        String receiverName = receiver.getName();
        PlayerTellEvent event = plugin.sendMessageFromPlayer(
            senderName, receiverName, message);

        if(!event.isCancelled())
        {
            // In case the message was modified, update it
            message = event.getMessage();

            String forReceiver = MessageUtils.formatForReceiver(senderName, message);
            receiver.sendMessage(forReceiver);

            String forSender = MessageUtils.formatForSender(receiverName, message);
            sender.sendMessage(forSender);
            return true;
        }
        else if(event.getCancelReason() != null)
        {
            sender.sendMessage(event.getCancelReason());
        }
        return false;
    }

    private boolean handleDiffServerTell(CommandSender sender, String receiverName, String message)
    {
        String senderName = sender.getName();
        PlayerTellEvent event = plugin.sendMessageFromPlayer(
            senderName, receiverName, message);

        if(!event.isCancelled())
        {
            // In case the message was modified, update it
            message = event.getMessage();

            String forSender = MessageUtils.formatForSender(receiverName, message);
            sender.sendMessage(forSender);

            String dataString = MessageUtils.toDataString(senderName, receiverName, message);
            deltaRedisApi.findPlayer(receiverName, cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    String destination = cachedPlayer.getServer();
                    deltaRedisApi.publish(destination, DeltaEssentialsChannels.TELL, dataString);
                }
                else
                {
                    CallbackUtil.sendMessage(senderName, Prefixes.FAILURE +
                        Prefixes.input(receiverName) + " is not online.");
                }
            });
            return true;
        }
        else if(event.getCancelReason() != null)
        {
            sender.sendMessage(event.getCancelReason());
        }
        return false;
    }

    private void setLastReplyTargetForReceiver(String senderName, String receiverName)
    {
        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(receiverName);
        if(dePlayer != null)
        {
            dePlayer.setLastReplyTarget(senderName);
        }
    }
}

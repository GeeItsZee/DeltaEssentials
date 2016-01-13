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
package com.yahoo.tracebachi.DeltaEssentials.Chat.Commands;

import com.yahoo.tracebachi.DeltaEssentials.CallbackUtil;
import com.yahoo.tracebachi.DeltaEssentials.Chat.ChatListener;
import com.yahoo.tracebachi.DeltaEssentials.Chat.DeltaChat;
import com.yahoo.tracebachi.DeltaEssentials.Chat.MessageUtils;
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTellEvent;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class ReplyCommand implements CommandExecutor
{
    private HashMap<String, String> replyMap;
    private DeltaRedisApi deltaRedisApi;
    private DeltaChat deltaChat;

    public ReplyCommand(HashMap<String, String> replyMap, DeltaRedisApi deltaRedisApi, DeltaChat deltaChat)
    {
        this.replyMap = replyMap;
        this.deltaRedisApi = deltaRedisApi;
        this.deltaChat = deltaChat;
    }

    public void shutdown()
    {
        this.deltaChat = null;
        this.deltaRedisApi = null;
        this.replyMap = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaEss.Tell.Use"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
            return true;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/tell <name> <message>");
            sender.sendMessage(Prefixes.INFO + "/reply <message>");
            return true;
        }

        String receiverName = replyMap.get(sender.getName());
        if(receiverName == null)
        {
            sender.sendMessage(Prefixes.FAILURE + "You have no one to reply to.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        boolean canUseColors = sender.hasPermission("DeltaEss.Tell.Color");

        // Check if the receiver is console
        if(receiverName.equalsIgnoreCase("console"))
        {
            handleConsoleTell(sender, message, canUseColors);
            return true;
        }

        // Check if the receiver is online on the same server
        Player receiver = Bukkit.getPlayer(receiverName);
        if(receiver != null && receiver.isOnline())
        {
            handleSameServerPlayerTell(sender, receiver, message, canUseColors);
        }
        else
        {
            handleDiffServerPlayerTell(sender, receiverName, message, canUseColors);
        }
        return true;
    }

    private void handleConsoleTell(CommandSender sender, String message, boolean canUseColors)
    {
        String senderName = sender.getName();
        PlayerTellEvent tellEvent = deltaChat.tellWithEvent(senderName, "Console",
            message, canUseColors);

        if(!tellEvent.isCancelled())
        {
            // In case the message was modified, update it
            message = tellEvent.getMessage();

            // Send messages
            String forSender = MessageUtils.formatForSender("Console", message);
            String forReceiver = MessageUtils.formatForReceiver(senderName, message);
            Bukkit.getConsoleSender().sendMessage(forReceiver);
            sender.sendMessage(forSender);

            // Insert into the reply map
            replyMap.put("CONSOLE", senderName);
        }
        else if(tellEvent.getCancelReason() != null)
        {
            sender.sendMessage(tellEvent.getCancelReason());
        }
    }

    private void handleSameServerPlayerTell(CommandSender sender, Player receiver,
        String message, boolean canUseColors)
    {
        String senderName = sender.getName();
        String receiverName = receiver.getName();
        PlayerTellEvent tellEvent = deltaChat.tellWithEvent(senderName, receiverName,
            message, canUseColors);

        if(!tellEvent.isCancelled())
        {
            // In case the message was modified, update it
            message = tellEvent.getMessage();

            // Send messages
            String forSender = MessageUtils.formatForSender(receiverName, message);
            String forReceiver = MessageUtils.formatForReceiver(senderName, message);
            receiver.sendMessage(forReceiver);
            sender.sendMessage(forSender);

            // Insert into the reply map
            replyMap.put(receiverName, senderName);
        }
        else if(tellEvent.getCancelReason() != null)
        {
            sender.sendMessage(tellEvent.getCancelReason());
        }
    }

    private void handleDiffServerPlayerTell(CommandSender sender, String receiverName,
        String message, boolean canUseColors)
    {
        String senderName = sender.getName();
        PlayerTellEvent tellEvent = deltaChat.tellWithEvent(senderName, receiverName,
            message, canUseColors);

        if(!tellEvent.isCancelled())
        {
            // In case the message was modified, update it
            message = tellEvent.getMessage();

            String forSender = MessageUtils.formatForSender(receiverName, message);
            String dataString = MessageUtils.toByteArrayDataString(senderName, receiverName, message);

            deltaRedisApi.findPlayer(receiverName, cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    String destination = cachedPlayer.getServer();
                    deltaRedisApi.publish(destination, ChatListener.TELL_CHANNEL, dataString);

                    CallbackUtil.sendMessage(senderName, forSender);
                }
                else
                {
                    CallbackUtil.sendMessage(senderName, Prefixes.FAILURE +
                        Prefixes.input(receiverName) + " is not online.");
                }
            });
        }
        else if(tellEvent.getCancelReason() != null)
        {
            sender.sendMessage(tellEvent.getCancelReason());
        }
    }
}

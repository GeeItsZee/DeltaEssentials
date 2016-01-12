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
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        if(!commandSender.hasPermission("DeltaEss.Tell.Use"))
        {
            commandSender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
            return true;
        }

        if(args.length < 1)
        {
            commandSender.sendMessage(Prefixes.INFO + "/tell <name> <message>");
            commandSender.sendMessage(Prefixes.INFO + "/reply <message>");
            return true;
        }

        String receiver = replyMap.get(commandSender.getName());
        if(receiver == null)
        {
            commandSender.sendMessage(Prefixes.FAILURE + "You have no one to reply to.");
            return true;
        }

        String sender = commandSender.getName();
        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        boolean canUseColors = commandSender.hasPermission("DeltaEss.Tell.Color");

        // Check if the receiver is CONSOLE
        if(receiver.equalsIgnoreCase("console"))
        {
            PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message, canUseColors);
            if(!tellEvent.isCancelled())
            {
                // In case the message was modified, update it
                message = tellEvent.getMessage();

                // Send messages
                Bukkit.getConsoleSender().sendMessage(MessageUtils.formatForReceiver(sender, message));
                commandSender.sendMessage(MessageUtils.formatForSender(receiver, message));
                replyMap.put("CONSOLE", sender);
            }
            else if(tellEvent.getCancelReason() != null)
            {
                commandSender.sendMessage(tellEvent.getCancelReason());
            }
            return true;
        }

        // Check if the receiver is a player on the same server
        Player receiverPlayer = Bukkit.getPlayer(receiver);
        if(receiverPlayer != null && receiverPlayer.isOnline())
        {
            receiver = receiverPlayer.getName();

            PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message, canUseColors);
            if(!tellEvent.isCancelled())
            {
                // In case the message was modified, update it
                message = tellEvent.getMessage();

                // Send messages
                receiverPlayer.sendMessage(MessageUtils.formatForReceiver(sender, message));
                commandSender.sendMessage(MessageUtils.formatForSender(receiver, message));
                replyMap.put(receiver, sender);
            }
            else if(tellEvent.getCancelReason() != null)
            {
                commandSender.sendMessage(tellEvent.getCancelReason());
            }
            return true;
        }

        // Check if the receiver might be on another server
        PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message, canUseColors);
        if(!tellEvent.isCancelled())
        {
            // In case the message was modified, update it
            message = tellEvent.getMessage();

            String finalReceiver = receiver;
            String formatted = MessageUtils.formatForSender(receiver, message);
            String dataString = MessageUtils.toByteArrayDataString(sender, receiver, message);

            deltaRedisApi.findPlayer(receiver, cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    String destination = cachedPlayer.getServer();
                    deltaRedisApi.publish(destination, ChatListener.TELL_CHANNEL, dataString);

                    replyMap.put(sender, finalReceiver);
                    CallbackUtil.sendMessage(sender, formatted);
                }
                else
                {
                    CallbackUtil.sendMessage(sender, Prefixes.FAILURE +
                        Prefixes.input(finalReceiver) + " is not online.");
                }
            });
        }
        else if(tellEvent.getCancelReason() != null)
        {
            commandSender.sendMessage(tellEvent.getCancelReason());
        }
        return true;
    }
}

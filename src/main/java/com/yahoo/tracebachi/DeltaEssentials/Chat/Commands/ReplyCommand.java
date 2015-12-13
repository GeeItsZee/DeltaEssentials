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

import com.yahoo.tracebachi.DeltaEssentials.Chat.DeltaChat;
import com.yahoo.tracebachi.DeltaEssentials.Chat.MessageUtils;
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTellEvent;
import com.yahoo.tracebachi.DeltaEssentials.Prefixes;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
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
        if(!commandSender.hasPermission("DeltaEss.Tell"))
        {
            commandSender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
            return true;
        }

        if(args.length <= 0)
        {
            commandSender.sendMessage(Prefixes.INFO + "/tell [name] [message]");
            commandSender.sendMessage(Prefixes.INFO + "/reply [message]");
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
        boolean allowColors = commandSender.hasPermission("DeltaEss.Tell.Color");

        if(receiver.equalsIgnoreCase("console"))
        {
            PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message);
            if(tellEvent.isCancelled()) { return true; }

            message = tellEvent.getMessage();
            String formatted = MessageUtils.format(sender, receiver, message, allowColors);
            Bukkit.getConsoleSender().sendMessage(formatted);
            commandSender.sendMessage(formatted);
            replyMap.put("CONSOLE", sender);
        }
        else
        {
            Player receiverPlayer = Bukkit.getPlayer(receiver);

            if(receiverPlayer != null && receiverPlayer.isOnline())
            {
                receiver = receiverPlayer.getName();

                PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message);
                if(tellEvent.isCancelled()) { return true; }

                message = tellEvent.getMessage();
                String formatted = MessageUtils.format(sender, receiver, message, allowColors);
                receiverPlayer.sendMessage(formatted);
                commandSender.sendMessage(formatted);
                replyMap.put(receiver, sender);
            }
            else
            {
                PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message);
                if(tellEvent.isCancelled()) { return true; }

                message = tellEvent.getMessage();
                String finalReceiver = receiver;
                String formatted = MessageUtils.format(sender, receiver, message, allowColors);
                String dataString = MessageUtils.toByteArrayDataString(
                    sender, receiver, allowColors, message);

                deltaRedisApi.findPlayer(receiver, cachedPlayer -> {
                    if(cachedPlayer != null)
                    {
                        String destination = cachedPlayer.getServer();
                        deltaRedisApi.publish(destination, DeltaChat.TELL_CHANNEL, dataString);

                        sendMessageFromCallback(sender, formatted);
                    }
                    else
                    {
                        sendMessageFromCallback(sender, Prefixes.FAILURE + finalReceiver + " is not online.");
                    }
                });
            }
        }
        return true;
    }

    private void sendMessageFromCallback(String playerName, String message)
    {
        Player player = Bukkit.getPlayer(playerName);

        if(player != null && player.isOnline())
        {
            player.sendMessage(message);
        }
    }
}

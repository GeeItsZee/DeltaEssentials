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
import com.yahoo.tracebachi.DeltaEssentials.Chat.DeltaChat;
import com.yahoo.tracebachi.DeltaEssentials.Chat.MessageUtils;
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTellEvent;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class TellCommand implements TabExecutor
{
    private HashMap<String, String> replyMap;
    private DeltaRedisApi deltaRedisApi;
    private DeltaChat deltaChat;

    public TellCommand(HashMap<String, String> replyMap, DeltaRedisApi deltaRedisApi, DeltaChat deltaChat)
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
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args)
    {
        List<String> result = new ArrayList<>();

        if(args.length != 0)
        {
            String lastArg = args[args.length - 1];
            for(String name : deltaRedisApi.getCachedPlayers())
            {
                if(name.startsWith(lastArg))
                {
                    result.add(name);
                }
            }
        }
        return result;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        if(!commandSender.hasPermission("DeltaEss.Tell"))
        {
            commandSender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
            return true;
        }

        if(args.length < 2)
        {
            commandSender.sendMessage(Prefixes.INFO + "/tell <name> <message>");
            commandSender.sendMessage(Prefixes.INFO + "/reply <message>");
            return true;
        }

        String sender = commandSender.getName();
        String receiver = args[0];
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        boolean canUseColors = commandSender.hasPermission("DeltaEss.Tell.Color");

        // Check if the receiver is CONSOLE
        if(receiver.equalsIgnoreCase("console"))
        {
            PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message);
            if(!tellEvent.isCancelled())
            {
                message = tellEvent.getMessage();
                String formatted = MessageUtils.format(sender, receiver, message, canUseColors);

                Bukkit.getConsoleSender().sendMessage(formatted);
                commandSender.sendMessage(formatted);
                replyMap.put("CONSOLE", sender);
                replyMap.put(sender, "CONSOLE");
            }
            return true;
        }

        // Check if the receiver is a player on the same server
        Player receiverPlayer = Bukkit.getPlayer(receiver);
        if(receiverPlayer != null && receiverPlayer.isOnline())
        {
            receiver = receiverPlayer.getName();

            PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message);
            if(!tellEvent.isCancelled())
            {
                message = tellEvent.getMessage();
                String formatted = MessageUtils.format(sender, receiver, message, canUseColors);

                receiverPlayer.sendMessage(formatted);
                commandSender.sendMessage(formatted);
                replyMap.put(receiver, sender);
                replyMap.put(sender, receiver);
            }
            return true;
        }

        // Check if the receiver might be on another server
        PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message);
        if(!tellEvent.isCancelled())
        {
            message = tellEvent.getMessage();
            String finalReceiver = receiver;
            String formatted = MessageUtils.format(sender, receiver, message, canUseColors);
            String dataString = MessageUtils.toByteArrayDataString(
                sender, receiver, message, canUseColors);

            deltaRedisApi.findPlayer(receiver, cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    String destination = cachedPlayer.getServer();
                    deltaRedisApi.publish(destination, DeltaChat.TELL_CHANNEL, dataString);

                    replyMap.put(sender, finalReceiver);
                    CallbackUtil.sendMessage(sender, formatted);
                }
                else
                {
                    CallbackUtil.sendMessage(sender,  Prefixes.FAILURE +
                        Prefixes.input(finalReceiver) + " is not online.");
                }
            });
        }
        return true;
    }
}

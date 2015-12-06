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
import com.yahoo.tracebachi.DeltaRedis.Shared.Cache.CachedPlayer;
import com.yahoo.tracebachi.DeltaRedis.Shared.Interfaces.DeltaRedisApi;
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
public class TellCommand implements CommandExecutor
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
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaEss.Tell"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to do that.");
            return true;
        }

        if(args.length <= 1)
        {
            sender.sendMessage(Prefixes.INFO + "/tell [name] [message]");
            sender.sendMessage(Prefixes.INFO + "/reply [message]");
            return true;
        }

        String receiverName = args[0].toLowerCase();
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String formatted;
        String senderName = sender.getName().toLowerCase();
        boolean allowColors = sender.hasPermission("DeltaEss.Tell.Color");
        PlayerTellEvent event = deltaChat.tellWithEvent(senderName, receiverName, message);

        // Call event
        if(!event.isCancelled())
        {
            message = event.getMessage();
            formatted = MessageUtils.format(senderName, receiverName, message, allowColors);
        }
        else { return true; }

        // Check if the receiver is console
        if(receiverName.equals("console"))
        {
            Bukkit.getConsoleSender().sendMessage(formatted);
            sender.sendMessage(formatted);

            replyMap.put(senderName, receiverName);
            replyMap.put(receiverName, senderName);
            return true;
        }

        // Check if the receiver is online
        Player receiver = Bukkit.getPlayer(receiverName);
        if(receiver != null && receiver.isOnline())
        {
            receiver.sendMessage(formatted);
            sender.sendMessage(formatted);

            replyMap.put(senderName, receiverName);
            replyMap.put(receiverName, senderName);
            return true;
        }

        // Check if the receiver is on another known server
        CachedPlayer cachedPlayer = deltaRedisApi.getPlayer(receiverName);
        if(cachedPlayer != null)
        {
            String dataString = MessageUtils.toByteArrayDataString(
                senderName, receiverName, allowColors, message);

            deltaRedisApi.publish(cachedPlayer.getServer(), "DeltaEss:Tell", dataString);
            sender.sendMessage(formatted);

            replyMap.put(senderName, receiverName);
            return true;
        }

        sender.sendMessage(Prefixes.FAILURE + receiverName + " is not online.");
        return true;
    }
}

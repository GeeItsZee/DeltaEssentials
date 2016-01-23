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
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTell extends DeltaEssentialsCommand
{
    private DeltaRedisApi deltaRedisApi;

    public CommandTell(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super("tell", "DeltaEss.Tell.Use", plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void shutdown()
    {
        this.deltaRedisApi = null;
        super.shutdown();
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfPlayerName(lastArg);
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length < 2)
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

        String receiverName = args[0];
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if(sender.hasPermission("DeltaEss.Tell.Color"))
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        // Check if the receiver is console
        if(receiverName.equalsIgnoreCase("console"))
        {
            if(handleConsoleTell(sender, message))
            {
                dePlayer.setLastReplyTarget(receiverName);
                setLastReplyTargetForReceiver(senderName, receiverName);
            }
            return;
        }

        // Try to auto complete a partial name
        receiverName = attemptAutoComplete(sender, receiverName);
        if(receiverName == null) { return; }

        // Check if the receiver is online on the same server
        Player receiver = Bukkit.getPlayer(receiverName);
        if(receiver != null && receiver.isOnline())
        {
            if(handleSameServerPlayerTell(sender, receiver, message))
            {
                dePlayer.setLastReplyTarget(receiverName);
                setLastReplyTargetForReceiver(senderName, receiverName);
            }
        }
        else
        {
            handleDiffServerPlayerTell(sender, receiverName, message);
            dePlayer.setLastReplyTarget(receiverName);
        }
    }

    private String attemptAutoComplete(CommandSender sender, String partial)
    {
        List<String> partialMatches = deltaRedisApi.matchStartOfPlayerName(partial);
        if(!partialMatches.contains(partial.toLowerCase()))
        {
            if(partialMatches.size() == 1)
            {
                return partialMatches.get(0);
            }
            else if(partialMatches.size() > 1)
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(partial) +
                    " matches too many players.");
                return null;
            }
        }
        return partial;
    }

    private boolean handleConsoleTell(CommandSender sender, String message)
    {
        String senderName = sender.getName();
        PlayerTellEvent tellEvent = plugin.sendMessageFromPlayer(
            senderName, "Console", message);

        if(!tellEvent.isCancelled())
        {
            // In case the message was modified, update it
            message = tellEvent.getMessage();

            String forReceiver = MessageUtils.formatForReceiver(senderName, message);
            Bukkit.getConsoleSender().sendMessage(forReceiver);

            String forSender = MessageUtils.formatForSender("Console", message);
            sender.sendMessage(forSender);
            return true;
        }
        else if(tellEvent.getCancelReason() != null)
        {
            sender.sendMessage(tellEvent.getCancelReason());
        }
        return false;
    }

    private boolean handleSameServerPlayerTell(CommandSender sender, Player receiver, String message)
    {
        String senderName = sender.getName();
        String receiverName = receiver.getName();
        PlayerTellEvent tellEvent = plugin.sendMessageFromPlayer(
            senderName, receiverName, message);

        if(!tellEvent.isCancelled())
        {
            // In case the message was modified, update it
            message = tellEvent.getMessage();

            String forReceiver = MessageUtils.formatForReceiver(senderName, message);
            receiver.sendMessage(forReceiver);

            String forSender = MessageUtils.formatForSender(receiverName, message);
            sender.sendMessage(forSender);
            return true;
        }
        else if(tellEvent.getCancelReason() != null)
        {
            sender.sendMessage(tellEvent.getCancelReason());
        }
        return false;
    }

    private boolean handleDiffServerPlayerTell(CommandSender sender, String receiverName, String message)
    {
        String senderName = sender.getName();
        PlayerTellEvent tellEvent = plugin.sendMessageFromPlayer(
            senderName, receiverName, message);

        if(!tellEvent.isCancelled())
        {
            // In case the message was modified, update it
            message = tellEvent.getMessage();

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
        else if(tellEvent.getCancelReason() != null)
        {
            sender.sendMessage(tellEvent.getCancelReason());
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

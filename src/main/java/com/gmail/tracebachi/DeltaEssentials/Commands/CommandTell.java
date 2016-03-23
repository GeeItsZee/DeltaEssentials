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
import com.gmail.tracebachi.DeltaEssentials.Listeners.TellChatListener;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTell implements TabExecutor, Registerable, Shutdownable
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
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("tell").setExecutor(null);
        plugin.getCommand("tell").setTabCompleter(null);

        plugin.getCommand("reply").setExecutor(null);
        plugin.getCommand("reply").setTabCompleter(null);
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
        DeltaEssPlayerData senderPlayerData = playerMap.get(senderName);

        if(senderPlayerData == null)
        {
            sender.sendMessage(Settings.format("PlayerDataNotLoaded"));
            return true;
        }

        if(commandName.equalsIgnoreCase("reply"))
        {
            receiverName = senderPlayerData.getReplyTo();

            if(receiverName.equals(""))
            {
                sender.sendMessage(Settings.format("NoReplyTarget"));
                return true;
            }

            message = String.join(" ", (CharSequence[]) Arrays.copyOfRange(args, 0, args.length));
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

            message = String.join(" ", (CharSequence[]) Arrays.copyOfRange(args, 1, args.length));
        }

        if(sender.hasPermission("DeltaEss.Tell.Color"))
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        TellChatListener tellChatListener = plugin.getTellChatListener();

        if(receiverName.equals("console"))
        {
            tellChatListener.sendMessage(
                senderName, sender,
                receiverName, Bukkit.getConsoleSender(),
                message, true);

            return true;
        }

        Player receiver = Bukkit.getPlayer(receiverName);

        if(receiver != null)
        {
            DeltaEssPlayerData receiverPlayerData = playerMap.get(receiverName);

            if(receiverPlayerData != null && receiverPlayerData.isVanishEnabled())
            {
                MessageUtil.sendMessage(senderName,
                    Settings.format("PlayerOffline", receiverName));
            }
            else
            {
                tellChatListener.sendMessage(
                    senderName, sender,
                    receiverName, receiver,
                    message, true);
            }

            return true;
        }

        boolean result = tellChatListener.sendMessage(
            senderName, sender,
            receiverName, null,
            message, true);

        if(result)
        {
            String finalMessage = message;

            deltaRedisApi.findPlayer(receiverName, cachedPlayer ->
            {
                if(cachedPlayer == null)
                {
                    MessageUtil.sendMessage(senderName,
                        Settings.format("PlayerOffline", receiverName));
                    return;
                }

                // Format: SenderName/\ReceiverName/\Message
                deltaRedisApi.publish(cachedPlayer.getServer(),
                    DeltaEssentialsChannels.TELL,
                    senderName, receiverName, finalMessage);
            });
        }

        return true;
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

        sender.sendMessage(Settings.format("TooManyAutoCompleteMatches", partial));
        return null;
    }
}

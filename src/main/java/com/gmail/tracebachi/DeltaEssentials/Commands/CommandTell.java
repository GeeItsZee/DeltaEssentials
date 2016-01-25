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
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssentialsPlayer;
import com.gmail.tracebachi.DeltaEssentials.Utils.CallbackUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
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

        Settings settings = plugin.getSettings();
        String senderName = sender.getName();
        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(senderName);

        if(dePlayer == null)
        {
            String playerDataNotLoaded = settings.format("PlayerDataNotLoaded");
            sender.sendMessage(playerDataNotLoaded);
            return;
        }

        String receiverName = args[0];
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if(sender.hasPermission("DeltaEss.Tell.Color"))
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        if(receiverName.equalsIgnoreCase("console"))
        {
            plugin.getChatListener().sendMessageFromPlayer(
                senderName, sender,
                "console", Bukkit.getConsoleSender(),
                message);
            return;
        }

        receiverName = attemptAutoComplete(sender, receiverName);

        if(receiverName != null)
        {
            Player receiver = Bukkit.getPlayer(receiverName);

            if(receiver != null)
            {
                plugin.getChatListener().sendMessageFromPlayer(
                    senderName, sender,
                    receiverName, receiver,
                    message);
            }
            else
            {
                checkDiffServer(sender, receiverName, message);
            }
        }
    }

    private void checkDiffServer(CommandSender sender, String receiverName, String message)
    {
        Settings settings = plugin.getSettings();
        String senderName = sender.getName();
        boolean tellSuccess = plugin.getChatListener().sendMessageFromPlayer(
            senderName, sender, receiverName, null, message);

        if(tellSuccess)
        {
            deltaRedisApi.findPlayer(receiverName, cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    String destination = cachedPlayer.getServer();
                    deltaRedisApi.publish(destination,
                        DeltaEssentialsChannels.TELL,
                        senderName, receiverName, message);
                }
                else
                {
                    String playerNotOnline = settings.format(
                        "PlayerNotOnline", receiverName);

                    CallbackUtil.sendMessage(senderName, playerNotOnline);
                }
            });
        }
    }

    private String attemptAutoComplete(CommandSender sender, String partial)
    {
        Settings settings = plugin.getSettings();
        List<String> partialMatches = deltaRedisApi.matchStartOfPlayerName(partial);

        if(!partialMatches.contains(partial.toLowerCase()))
        {
            if(partialMatches.size() == 1)
            {
                return partialMatches.get(0);
            }
            else if(partialMatches.size() > 1)
            {
                String tooManyAutoCompleteMatches = settings.format(
                    "TooManyAutoCompleteMatches", partial);

                sender.sendMessage(tooManyAutoCompleteMatches);
                return null;
            }
        }
        return partial;
    }
}

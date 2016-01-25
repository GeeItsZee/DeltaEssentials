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

        Settings settings = plugin.getSettings();
        String senderName = sender.getName();
        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(senderName);

        if(dePlayer == null)
        {
            String playerDataNotLoaded = settings.format("PlayerDataNotLoaded");
            sender.sendMessage(playerDataNotLoaded);
            return;
        }

        String receiverName = dePlayer.getLastReplyTarget();

        if(receiverName.equals(""))
        {
            String onNoReplyTarget = settings.format("OnNoReplyTarget");
            sender.sendMessage(onNoReplyTarget);
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

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
}

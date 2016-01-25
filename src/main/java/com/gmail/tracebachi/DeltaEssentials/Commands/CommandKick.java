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
import com.gmail.tracebachi.DeltaEssentials.Utils.CallbackUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandKick extends DeltaEssentialsCommand
{
    private DeltaRedisApi deltaRedisApi;

    public CommandKick(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super("kick", "DeltaEss.Kick", plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void shutdown()
    {
        this.deltaRedisApi = null;
        super.shutdown();
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfPlayerName(lastArg);
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/kick <player> [message]");
            return;
        }

        Settings settings = plugin.getSettings();
        String nameToKick = args[0];
        String senderName = sender.getName();
        String reason = getKickReason(args);
        Player playerToKick = Bukkit.getPlayer(nameToKick);

        if(playerToKick != null)
        {
            String onKickPlayer = settings.format("OnKickPlayer", senderName, reason);
            playerToKick.kickPlayer(onKickPlayer);
            announceKick(settings, nameToKick, senderName, reason);
        }
        else
        {
            deltaRedisApi.findPlayer(nameToKick, cachedPlayer ->
            {
                if(cachedPlayer != null)
                {
                    deltaRedisApi.publish(Servers.SPIGOT,
                        DeltaEssentialsChannels.KICK,
                        senderName, nameToKick, reason);

                    announceKick(settings, nameToKick, senderName, reason);
                }
                else
                {
                    String playerNotOnline = settings.format(
                        "PlayerNotOnline", nameToKick);

                    CallbackUtil.sendMessage(senderName, playerNotOnline);
                }
            });
        }
    }

    private String getKickReason(String[] args)
    {
        if(args.length > 1)
        {
            String joined = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            return ChatColor.translateAlternateColorCodes('&', joined);
        }
        else
        {
            return "Kicked from server!";
        }
    }

    private void announceKick(Settings settings, String nameToKick, String senderName, String reason)
    {
        String onKickAnnounce = settings.format("OnKickAnnounce", senderName, nameToKick, reason);

        for(Player onlinePlayer : Bukkit.getOnlinePlayers())
        {
            onlinePlayer.sendMessage(onKickAnnounce);
        }
    }
}

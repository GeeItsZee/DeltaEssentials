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

import com.earth2me.essentials.utils.DateUtil;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsChannels;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Utils.CallbackUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandJail extends DeltaEssentialsCommand
{
    private DeltaRedisApi deltaRedisApi;

    public CommandJail(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super("jail", "DeltaEss.Jail", plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void shutdown()
    {
        this.deltaRedisApi = null;
        super.shutdown();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfPlayerName(lastArg);
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/jail <player> <jail name> [date diff]");
            return;
        }

        Settings settings = plugin.getSettings();
        String jailServer = settings.getJailServer();
        String senderName = sender.getName();
        String toJail = args[0];
        String jailName = "";
        String dateDiff = "";
        Player playerToJail = Bukkit.getPlayer(toJail);

        if(args.length >= 2)
        {
            jailName = args[1];
            if(!settings.isValidJail(jailName))
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(jailName) +
                    " is not a valid jail.");
                return;
            }
        }

        if(args.length >= 3)
        {
            dateDiff = args[2];
            if(!isValidDateDifference(dateDiff))
            {
                sender.sendMessage(Prefixes.FAILURE + Prefixes.input(dateDiff) +
                    " is not a valid time difference.");
                return;
            }
        }

        if(deltaRedisApi.getServerName().equals(jailServer))
        {
            String joined = String.join(" ", Arrays.asList(args));
            Bukkit.dispatchCommand(sender, "essentials:jail " + joined);

            if(playerToJail == null)
            {
                moveToJailServer(toJail, senderName, jailServer);
            }
        }
        else
        {
            deltaRedisApi.publish(jailServer, DeltaEssentialsChannels.JAIL,
                senderName, toJail + " " + jailName + " " + dateDiff);

            if(playerToJail != null)
            {
                plugin.sendToServer(playerToJail, jailServer);
            }
            else
            {
                moveToJailServer(toJail, senderName, jailServer);
            }
        }
    }

    private void moveToJailServer(String toJail, String senderName, String jailServer)
    {
        deltaRedisApi.findPlayer(toJail, cachedPlayer ->
        {
            if(cachedPlayer != null)
            {
                deltaRedisApi.publish(cachedPlayer.getServer(),
                    DeltaEssentialsChannels.MOVE,
                    senderName, toJail, jailServer);
            }
            else
            {
                Settings settings = plugin.getSettings();
                String onPlayerNotFound = settings.format("OnPlayerNotFound", toJail);

                CallbackUtil.sendMessage(senderName, onPlayerNotFound);
            }
        });
    }

    private boolean isValidDateDifference(String arg)
    {
        try
        {
            DateUtil.parseDateDiff(arg, true);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
}

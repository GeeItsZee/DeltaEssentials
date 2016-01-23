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
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssentialsPlayer;
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandSocialSpy extends DeltaEssentialsCommand
{
    public CommandSocialSpy(DeltaEssentials plugin)
    {
        super("socialspy", "DeltaEss.SocialSpy", plugin);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args)
    {
        return Arrays.asList("on", "off");
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can use SocialSpy. Console has chat logs.");
            return;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/socialspy <on|off>");
            return;
        }

        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(sender.getName());

        if(dePlayer == null)
        {
            sender.sendMessage(Prefixes.FAILURE + "Your data has not been loaded!");
            return;
        }

        if(args[0].equalsIgnoreCase("on"))
        {
            dePlayer.setSocialSpyEnabled(true);
            sender.sendMessage(Prefixes.SUCCESS + "SocialSpy " + Prefixes.input("enabled"));
        }
        else if(args[0].equalsIgnoreCase("off"))
        {
            dePlayer.setSocialSpyEnabled(false);
            sender.sendMessage(Prefixes.SUCCESS + "SocialSpy " + Prefixes.input("disabled"));
        }
        else
        {
            sender.sendMessage(Prefixes.INFO + "/socialspy <on|off>");
        }
    }
}

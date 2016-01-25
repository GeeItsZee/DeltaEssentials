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
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTpDeny extends DeltaEssentialsCommand
{
    public CommandTpDeny(DeltaEssentials plugin)
    {
        super("tpdeny", "DeltaEss.TpDeny", plugin);
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
            sender.sendMessage(Prefixes.FAILURE + "Only players can deny teleports.");
            return;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.FAILURE + "/tpdeny <on|off>");
            return;
        }

        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(sender.getName());

        if(dePlayer == null)
        {
            String playerDataNotLoaded = plugin.getSettings().format("PlayerDataNotLoaded");
            sender.sendMessage(playerDataNotLoaded);
            return;
        }

        if(args[0].equalsIgnoreCase("on"))
        {
            dePlayer.setTeleportDenyEnabled(true);
            sender.sendMessage(Prefixes.SUCCESS + "TpDeny " + Prefixes.input("enabled"));
        }
        else if(args[0].equalsIgnoreCase("off"))
        {
            dePlayer.setTeleportDenyEnabled(false);
            sender.sendMessage(Prefixes.SUCCESS + "TpDeny " + Prefixes.input("disabled"));
        }
        else
        {
            sender.sendMessage(Prefixes.FAILURE + "/tpdeny <on|off>");
        }
    }
}

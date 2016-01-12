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
package com.yahoo.tracebachi.DeltaEssentials.Commands;

import com.yahoo.tracebachi.DeltaEssentials.DeltaEssentialsPlugin;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/4/15.
 */
public class JoinStopCommand implements CommandExecutor, Listener
{
    private DeltaEssentialsPlugin plugin;

    public JoinStopCommand(DeltaEssentialsPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaEss.JoinStop"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use that command.");
            return true;
        }

        if(args.length >= 1)
        {
            switch(args[0].toLowerCase())
            {
                case "on":
                    plugin.setJoinStop(true);
                    break;
                case "off":
                    plugin.setJoinStop(false);
                    break;
                default:
                    plugin.setJoinStop(!plugin.isJoinStopEnabled());
                    break;
            }
        }
        else
        {
            plugin.setJoinStop(!plugin.isJoinStopEnabled());
        }

        if(plugin.isJoinStopEnabled())
        {
            sender.sendMessage(Prefixes.SUCCESS + "JoinStop " + Prefixes.input("enabled"));
        }
        else
        {
            sender.sendMessage(Prefixes.SUCCESS + "JoinStop " + Prefixes.input("disabled"));
        }

        return true;
    }
}

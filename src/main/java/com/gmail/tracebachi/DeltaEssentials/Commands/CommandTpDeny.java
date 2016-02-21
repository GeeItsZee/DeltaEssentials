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
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayer;
import com.gmail.tracebachi.DeltaEssentials.Utils.CommandMessageUtil;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandTpDeny implements TabExecutor, Registerable, Shutdownable
{
    private DeltaEssentials plugin;

    public CommandTpDeny(DeltaEssentials plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("tpdeny").setExecutor(this);
        plugin.getCommand("tpdeny").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("tpdeny").setExecutor(null);
        plugin.getCommand("tpdeny").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args)
    {
        return Arrays.asList("on", "off");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/tpdeny <on|off>");
            return true;
        }

        if(!(sender instanceof Player))
        {
            CommandMessageUtil.onlyForPlayers(sender, "tpdeny");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.TpDeny"))
        {
            CommandMessageUtil.noPermission(sender, "DeltaEss.TpDeny");
            return true;
        }

        DeltaEssPlayer dePlayer = plugin.getPlayerMap().get(sender.getName());

        if(dePlayer == null)
        {
            sender.sendMessage(Prefixes.FAILURE + "Player data has not been loaded.");
            return true;
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
            sender.sendMessage(Prefixes.INFO + "/tpdeny <on|off>");
        }

        return true;
    }
}

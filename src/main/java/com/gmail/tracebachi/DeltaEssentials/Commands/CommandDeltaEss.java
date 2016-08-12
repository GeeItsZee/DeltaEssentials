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
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class CommandDeltaEss implements TabExecutor, Registerable, Shutdownable
{
    private DeltaEssentials plugin;

    public CommandDeltaEss(DeltaEssentials plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("deltaessentials").setExecutor(this);
        plugin.getCommand("deltaessentials").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("deltaessentials").setExecutor(null);
        plugin.getCommand("deltaessentials").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command,
                                      String s, String[] args)
    {
        return Arrays.asList("on", "off");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Settings.format("DeltaEssentialsUsage"));
            return true;
        }

        if(args[0].equalsIgnoreCase("debug"))
        {
            if(!sender.hasPermission("DeltaEss.Debug"))
            {
                sender.sendMessage(Settings.format("NoPermission", "DeltaEss.Debug"));
                return true;
            }

            if(args.length < 2)
            {
                sender.sendMessage(Settings.format("DeltaEssentialsDebugUsage"));
                return true;
            }

            if(args[0].equalsIgnoreCase("on"))
            {
                Settings.setDebugEnabled(true);
                sender.sendMessage(Settings.format("DeltaEssentialsDebugChange", "ON"));
            }
            else if(args[0].equalsIgnoreCase("off"))
            {
                Settings.setDebugEnabled(false);
                sender.sendMessage(Settings.format("DeltaEssentialsDebugChange", "OFF"));
            }
            else
            {
                sender.sendMessage(Settings.format("DeltaEssentialsDebugUsage"));
            }
        }
        else if(args[0].equalsIgnoreCase("reload"))
        {
            if(!sender.hasPermission("DeltaEss.Reload"))
            {
                sender.sendMessage(Settings.format("NoPermission", "DeltaEss.Reload"));
                return true;
            }

            plugin.reloadConfig();
            Settings.read(plugin.getConfig());

            sender.sendMessage(Settings.format("DeltaEssentialsReloaded"));
        }
        else
        {
            sender.sendMessage(Settings.format("DeltaEssentialsUsage"));
            return true;
        }

        return true;
    }
}

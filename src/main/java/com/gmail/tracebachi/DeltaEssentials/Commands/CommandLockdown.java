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
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandLockdown implements TabExecutor, Shutdownable, Registerable, Listener
{
    private boolean isOnLockdown;
    private DeltaEssentials plugin;

    public CommandLockdown(DeltaEssentials plugin)
    {
        this.plugin = plugin;
        this.isOnLockdown = plugin.getSettings().isStartWithLockdown();
    }

    @Override
    public void register()
    {
        plugin.getCommand("lockdown").setExecutor(this);
        plugin.getCommand("lockdown").setTabCompleter(this);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("lockdown").setExecutor(null);
        plugin.getCommand("lockdown").setTabCompleter(null);

        HandlerList.unregisterAll(this);
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
            sender.sendMessage(Prefixes.INFO + "/lockdown <on|off>");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Lockdown"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaEss.Lockdown") + " permission.");
            return true;
        }

        if(args[0].equalsIgnoreCase("on"))
        {
            isOnLockdown = true;

            sender.sendMessage(Prefixes.SUCCESS + "Lockdown " + Prefixes.input("enabled"));
        }
        else if(args[0].equalsIgnoreCase("off"))
        {
            isOnLockdown = false;

            sender.sendMessage(Prefixes.SUCCESS + "Lockdown " + Prefixes.input("disabled"));
        }
        else
        {
            sender.sendMessage(Prefixes.INFO + "/lockdown <on|off>");
        }

        return true;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerLoginEvent(PlayerLoginEvent event)
    {
        if(isOnLockdown)
        {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                plugin.getSettings().getLockdownMessage());
        }
    }
}

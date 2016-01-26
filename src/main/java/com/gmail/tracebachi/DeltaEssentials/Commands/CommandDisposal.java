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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandDisposal implements TabExecutor, Shutdownable, Registerable
{
    private DeltaEssentials plugin;

    public CommandDisposal(DeltaEssentials plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("disposal").setExecutor(this);
        plugin.getCommand("disposal").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("disposal").setExecutor(null);
        plugin.getCommand("disposal").setTabCompleter(null);
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
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can open the disposal.");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Disposal"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaEss.Disposal") + " permission.");
            return true;
        }

        Player player = (Player) sender;
        Inventory inventory = plugin.getServer().createInventory(player, 36, "Black Hole");

        player.openInventory(inventory);
        return true;
    }
}

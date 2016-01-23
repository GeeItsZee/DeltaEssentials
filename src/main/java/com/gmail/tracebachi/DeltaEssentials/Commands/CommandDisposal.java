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
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandDisposal extends DeltaEssentialsCommand
{
    public CommandDisposal(DeltaEssentials plugin)
    {
        super("disposal", "DeltaEss.Disposal", plugin);
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(sender instanceof Player)
        {
            Player player = (Player) sender;
            Inventory inventory = plugin.getServer().createInventory(player, 36, "Disposal");
            player.openInventory(inventory);
        }
        else
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can open the disposal.");
        }
    }
}

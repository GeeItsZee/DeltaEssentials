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
package com.yahoo.tracebachi.DeltaEssentials.Chat.Commands;

import com.yahoo.tracebachi.DeltaEssentials.Chat.DeltaChat;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/4/15.
 */
public class SocialSpyCommand implements CommandExecutor, Listener
{
    private DeltaChat deltaChat;

    public SocialSpyCommand(DeltaChat deltaChat)
    {
        this.deltaChat = deltaChat;
    }

    public void shutdown()
    {
        this.deltaChat = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only players can use SocialSpy. Console has chat logs.");
            return true;
        }

        if(!sender.hasPermission("DeltaEss.SocialSpy"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use that command.");
            return true;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/socialspy <on|off>");
            return true;
        }

        if(args[0].equalsIgnoreCase("on"))
        {
            deltaChat.addSocialSpy(sender.getName());
            sender.sendMessage(Prefixes.SUCCESS + "SocialSpy " + Prefixes.input("enabled"));
        }
        else if(args[0].equalsIgnoreCase("off"))
        {
            deltaChat.removeSocialSpy(sender.getName());
            sender.sendMessage(Prefixes.SUCCESS + "SocialSpy " + Prefixes.input("disabled"));
        }
        else
        {
            sender.sendMessage(Prefixes.INFO + "/socialspy <on|off>");
        }
        return true;
    }
}

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
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.*;

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
            sender.sendMessage(formatUsage("/tpdeny <on|off>"));
            return true;
        }

        if(!(sender instanceof Player))
        {
            sender.sendMessage(formatPlayerOnlyCommand("/tpdeny"));
            return true;
        }

        if(!sender.hasPermission("DeltaEss.Tp.Deny"))
        {
            sender.sendMessage(formatNoPerm("DeltaEss.Tp.Deny"));
            return true;
        }

        DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(sender.getName());
        if(playerData == null)
        {
            sender.sendMessage(format("DeltaEss.PlayerDataNotLoaded"));
            return true;
        }

        if(args[0].equalsIgnoreCase("on"))
        {
            playerData.setDenyingTeleports(true);
            sender.sendMessage(format("DeltaEss.SettingChanged", "TpDeny", "On"));
        }
        else if(args[0].equalsIgnoreCase("off"))
        {
            playerData.setDenyingTeleports(false);
            sender.sendMessage(format("DeltaEss.SettingChanged", "TpDeny", "OFF"));
        }
        else
        {
            sender.sendMessage(formatUsage("/tpdeny <on|off>"));
        }

        return true;
    }
}

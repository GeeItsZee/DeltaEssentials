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
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.SocialSpyLevel;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/4/15.
 */
public class CommandSocialSpy implements TabExecutor, Registerable, Shutdownable
{
    private DeltaEssentials plugin;

    public CommandSocialSpy(DeltaEssentials plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("socialspy").setExecutor(this);
        plugin.getCommand("socialspy").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("socialspy").setExecutor(null);
        plugin.getCommand("socialspy").setTabCompleter(null);
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
        return Arrays.asList("all", "world", "off");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(Settings.format("SocialSpyUsage"));
            return true;
        }

        if(!(sender instanceof Player))
        {
            sender.sendMessage(Settings.format("PlayersOnly", "/socialspy"));
            return true;
        }

        if(!sender.hasPermission("DeltaEss.SocialSpy"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaEss.SocialSpy"));
            return true;
        }

        DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(sender.getName());

        if(playerData == null)
        {
            sender.sendMessage(Settings.format("PlayerDataNotLoaded"));
            return true;
        }

        if(args[0].equalsIgnoreCase("all"))
        {
            playerData.setSocialSpyLevel(SocialSpyLevel.ALL);
            sender.sendMessage(Settings.format("SocialSpyChange", "ALL"));
        }
        else if(args[0].equalsIgnoreCase("world"))
        {
            playerData.setSocialSpyLevel(SocialSpyLevel.WORLD);
            sender.sendMessage(Settings.format("SocialSpyChange", "WORLD"));
        }
        else if(args[0].equalsIgnoreCase("none"))
        {
            playerData.setSocialSpyLevel(SocialSpyLevel.NONE);
            sender.sendMessage(Settings.format("SocialSpyChange", "NONE"));
        }
        else
        {
            sender.sendMessage(Settings.format("SocialSpyUsage"));
        }

        return true;
    }
}

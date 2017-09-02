/*
 * DeltaEssentials - Player data, chat, and teleport plugin for BungeeCord and Spigot servers
 * Copyright (C) 2017 tracebachi@gmail.com (GeeItsZee)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaEssentials.Spigot.Teleport;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.FormatNames;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Permissions;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.TabCompleteNameHelper;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TpCommand implements TabExecutor, Registerable
{
  private static final String COMMAND_NAME = "tp";
  private static final String COMMAND_USAGE = "/tp <name>";
  private static final String TP_SELF_TO_OTHER_PERM = "DeltaEss.Tp.MeToOther";
  private static final String TP_OTHER_TO_OTHER_PERM = "DeltaEss.Tp.OtherToOther";

  private final DeltaEssentialsPlugin plugin;
  private final TpToManager tpToManager;
  private final MessageFormatMap formatMap;
  private final SockExchangeApi api;

  public TpCommand(
    DeltaEssentialsPlugin plugin, TpToManager tpToManager, MessageFormatMap formatMap,
    SockExchangeApi api)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(tpToManager, "tpToManager");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(api, "api");

    this.plugin = plugin;
    this.tpToManager = tpToManager;
    this.formatMap = formatMap;
    this.api = api;
  }

  @Override
  public void register()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(this);
    plugin.getCommand(COMMAND_NAME).setTabCompleter(this);
  }

  @Override
  public void unregister()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(null);
    plugin.getCommand(COMMAND_NAME).setTabCompleter(null);
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
  {
    return TabCompleteNameHelper.getNamesThatStartsWith(args[args.length - 1], api);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (args.length < 1)
    {
      sender.sendMessage(formatMap.format(FormatNames.USAGE, COMMAND_USAGE));
      return true;
    }

    if (args.length == 1)
    {
      if (!(sender instanceof Player))
      {
        sender.sendMessage(formatMap.format(FormatNames.PLAYER_ONLY_COMMAND, COMMAND_NAME));
        return true;
      }

      if (!sender.hasPermission(TP_SELF_TO_OTHER_PERM))
      {
        sender.sendMessage(formatMap.format(FormatNames.NO_PERM, TP_SELF_TO_OTHER_PERM));
        return true;
      }

      DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(sender.getName());

      if (playerFile == null)
      {
        sender.sendMessage(formatMap.format(FormatNames.PLAYER_FILE_NOT_LOADED));
        return true;
      }

      boolean ignoreBlocking = sender.hasPermission(Permissions.TP_IGNORE_BLOCKING_PERM);
      boolean ignoreVanish = sender.hasPermission(Permissions.TP_IGNORE_VANISH_PERM);
      String destPlayerName = args[0];
      tpToManager.tpTo((Player) sender, destPlayerName, ignoreBlocking, ignoreVanish);
    }
    else
    {
      String startPlayerName = args[0];
      String destPlayerName = args[1];
      Player startPlayer = plugin.getServer().getPlayerExact(startPlayerName);

      if (startPlayer == null)
      {
        sender.sendMessage(formatMap.format(FormatNames.PLAYER_OFFLINE, startPlayerName));
        return true;
      }

      if (!sender.hasPermission(TP_OTHER_TO_OTHER_PERM))
      {
        sender.sendMessage(formatMap.format(FormatNames.NO_PERM, TP_OTHER_TO_OTHER_PERM));
        return true;
      }

      if (sender instanceof Player)
      {
        DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(sender.getName());

        if (playerFile == null)
        {
          sender.sendMessage(formatMap.format(FormatNames.PLAYER_FILE_NOT_LOADED));
          return true;
        }
      }

      tpToManager.tpOtherToOther(startPlayer, destPlayerName);
    }

    return true;
  }
}

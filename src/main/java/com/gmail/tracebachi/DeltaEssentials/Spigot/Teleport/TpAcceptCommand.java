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
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TpAcceptCommand implements CommandExecutor, Registerable
{
  private static final String COMMAND_NAME = "tpaccept";
  private static final String COMMAND_PERM = "DeltaEss.Tp.OtherToMe.Answer";

  private final DeltaEssentialsPlugin plugin;
  private final TpHereManager tpHereManager;
  private final MessageFormatMap formatMap;

  public TpAcceptCommand(
    DeltaEssentialsPlugin plugin, TpHereManager tpHereManager, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(tpHereManager, "tpHereManager");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.plugin = plugin;
    this.tpHereManager = tpHereManager;
    this.formatMap = formatMap;
  }

  @Override
  public void register()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(this);
  }

  @Override
  public void unregister()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(null);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (!(sender instanceof Player))
    {
      sender.sendMessage(formatMap.format(FormatNames.PLAYER_ONLY_COMMAND, COMMAND_NAME));
      return true;
    }

    if (!sender.hasPermission(COMMAND_PERM))
    {
      sender.sendMessage(formatMap.format(FormatNames.NO_PERM, COMMAND_PERM));
      return true;
    }

    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(sender.getName());

    if (playerFile == null)
    {
      sender.sendMessage(formatMap.format(FormatNames.PLAYER_FILE_NOT_LOADED));
      return true;
    }

    tpHereManager.acceptTpAskHere((Player) sender);
    return true;
  }
}

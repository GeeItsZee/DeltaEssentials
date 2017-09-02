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
package com.gmail.tracebachi.DeltaEssentials.Spigot.Chat;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.FormatNames;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.TabCompleteNameHelper;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TellCommand extends AbstractTellCommand implements TabExecutor, Registerable
{
  private static final String COMMAND_NAME = "tell";
  private static final String COMMAND_USAGE = "/tell <name> <message>";

  public TellCommand(
    DeltaEssentialsPlugin plugin, TellChatManager tellChatManager,
    MessageFormatMap messageFormatMap, SockExchangeApi api)
  {
    super(plugin, tellChatManager, messageFormatMap, api);
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
  public List<String> onTabComplete(
    CommandSender commandSender, Command command, String s, String[] args)
  {
    return TabCompleteNameHelper.getNamesThatStartsWith(args[args.length - 1], api);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (!sender.hasPermission(COMMAND_PERM))
    {
      sender.sendMessage(formatMap.format(FormatNames.NO_PERM, COMMAND_PERM));
      return true;
    }

    if (args.length < 2)
    {
      sender.sendMessage(formatMap.format(FormatNames.USAGE, COMMAND_USAGE));
      return true;
    }

    String receiverName = args[0];
    List<String> names = TabCompleteNameHelper.getNamesThatStartsWith(args[0], api);

    if (names.size() > 1)
    {
      sender.sendMessage(formatMap.format(FormatNames.TOO_MANY_MATCHING_NAMES, args[0]));
      return true;
    }

    if (names.size() == 1)
    {
      receiverName = names.get(0);
    }

    String senderName = sender.getName();

    if (sender instanceof Player)
    {
      DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(senderName);

      if (playerFile == null)
      {
        sender.sendMessage(formatMap.format(FormatNames.PLAYER_FILE_NOT_LOADED));
        return true;
      }

      playerFile.setReplyingTo(receiverName);
    }

    String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

    if (sender.hasPermission(TELL_USE_COLOR_PERM))
    {
      message = ChatColor.translateAlternateColorCodes('&', message);
    }

    boolean ignoreVanish = sender.hasPermission(TELL_IGNORE_VANISH_PERM);
    tellChatManager.sendMessage(senderName, receiverName, message, ignoreVanish);
    return true;
  }
}

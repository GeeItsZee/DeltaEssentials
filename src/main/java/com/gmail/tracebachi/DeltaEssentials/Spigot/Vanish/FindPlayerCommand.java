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
package com.gmail.tracebachi.DeltaEssentials.Spigot.Vanish;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Channels;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.FormatNames;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ResponseMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class FindPlayerCommand implements CommandExecutor, Registerable
{
  private static final String COMMAND_NAME = "findplayer";
  private static final String COMMAND_USAGE = "/findplayer <name>";
  private static final String PERM_COMMAND = "DeltaEss.FindPlayer";
  private static final String IGNORE_VANISH_PERM = "DeltaEss.FindPlayer.IgnoreVanish";

  private final DeltaEssentialsPlugin plugin;
  private final MessageFormatMap formatMap;
  private final SockExchangeApi api;
  private final Consumer<ReceivedMessage> findPlayerListener;

  public FindPlayerCommand(
    DeltaEssentialsPlugin plugin, MessageFormatMap formatMap, SockExchangeApi api)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(api, "api");

    this.plugin = plugin;
    this.formatMap = formatMap;
    this.api = api;
    this.findPlayerListener = (m) -> plugin.executeSync(() -> onFindPlayerChannelRequest(m));
  }

  @Override
  public void register()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(this);

    api.getMessageNotifier().register(Channels.FIND_PLAYER, findPlayerListener);
  }

  @Override
  public void unregister()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(null);

    api.getMessageNotifier().unregister(Channels.FIND_PLAYER, findPlayerListener);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (args.length < 1)
    {
      sender.sendMessage(formatMap.format(FormatNames.USAGE, COMMAND_USAGE));
      return true;
    }

    if (!sender.hasPermission(PERM_COMMAND))
    {
      sender.sendMessage(formatMap.format(FormatNames.NO_PERM, PERM_COMMAND));
      return true;
    }

    String senderName = sender.getName();
    String nameToFind = args[0];

    ByteArrayDataOutput out = ByteStreams.newDataOutput(nameToFind.length() * 6);
    out.writeUTF(senderName);
    out.writeUTF(api.getServerName());
    out.writeUTF(nameToFind);
    out.writeBoolean(sender.hasPermission(IGNORE_VANISH_PERM));

    // Send the message
    api.sendToServerOfPlayer(Channels.FIND_PLAYER, out.toByteArray(), nameToFind,
      (responseMessage) -> onResponseToSendMessage(senderName, nameToFind, responseMessage),
      TimeUnit.SECONDS.toMillis(5));
    return true;
  }

  private void onResponseToSendMessage(
    String senderName, String nameToFind, ResponseMessage responseMessage)
  {
    // If the request was not ok, assume the nameToFind is offline.
    if (!responseMessage.getResponseStatus().isOk())
    {
      String message = formatMap.format(FormatNames.PLAYER_OFFLINE, nameToFind);
      api.sendChatMessages(Collections.singletonList(message), senderName, api.getServerName());
    }
  }

  private void onFindPlayerChannelRequest(ReceivedMessage receivedMessage)
  {
    // Respond to indicate the message got to the server
    receivedMessage.respond();

    ByteArrayDataInput in = receivedMessage.getDataInput();
    String senderName = in.readUTF();
    String sourceServerName = in.readUTF();
    String nameToFind = in.readUTF();
    boolean ignoreVanish = in.readBoolean();

    Player playerToFind = plugin.getServer().getPlayerExact(nameToFind);

    // Check if player is online
    if (playerToFind == null)
    {
      String message = formatMap.format(FormatNames.PLAYER_OFFLINE, nameToFind);
      api.sendChatMessages(Collections.singletonList(message), senderName, sourceServerName);
      return;
    }

    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(nameToFind);

    // Check if player is vanished
    if (playerFile != null)
    {
      if (!ignoreVanish && playerFile.isVanished())
      {
        String message = formatMap.format(FormatNames.PLAYER_OFFLINE, nameToFind);
        api.sendChatMessages(Collections.singletonList(message), senderName, sourceServerName);
        return;
      }
    }

    String message = formatMap.format(FormatNames.PLAYER_ONLINE, nameToFind,
      api.getServerName());
    api.sendChatMessages(Collections.singletonList(message), senderName, sourceServerName);
  }
}

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

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Channels;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.FormatNames;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class SocialSpyListener implements Registerable
{
  private final DeltaEssentialsPlugin plugin;
  private final MessageFormatMap formatMap;
  private final SockExchangeApi api;
  private final Consumer<ReceivedMessage> socialSpyChannelListener;

  public SocialSpyListener(
    DeltaEssentialsPlugin plugin, MessageFormatMap formatMap, SockExchangeApi api)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(api, "api");

    this.plugin = plugin;
    this.formatMap = formatMap;
    this.api = api;
    this.socialSpyChannelListener = this::onSocialSpyChannelRequest;
  }

  @Override
  public void register()
  {
    api.getMessageNotifier().register(Channels.SOCIAL_SPY, socialSpyChannelListener);
  }

  @Override
  public void unregister()
  {
    api.getMessageNotifier().unregister(Channels.SOCIAL_SPY, socialSpyChannelListener);
  }

  public void sendMessageToSocialSpies(String senderName, String receiverName, String message)
  {
    ByteArrayDataOutput out = ByteStreams.newDataOutput(message.length() * 3);
    out.writeUTF(senderName);
    out.writeUTF(receiverName);
    out.writeUTF(message);
    api.sendToServers(Channels.SOCIAL_SPY, out.toByteArray());

    sendToSocialSpiesOnThisServer(senderName, receiverName, message, true);
  }

  private void onSocialSpyChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String senderName = in.readUTF();
    String receiverName = in.readUTF();
    String message = in.readUTF();

    plugin.executeSync(
      () -> sendToSocialSpiesOnThisServer(senderName, receiverName, message, false));
  }

  private void sendToSocialSpiesOnThisServer(
    String senderName, String receiverName, String message, boolean isFromCurrentServer)
  {
    String socialSpyMessage = formatMap.format(FormatNames.TELL_SOCIAL_SPY, senderName,
      receiverName, message);

    Server server = plugin.getServer();

    plugin.forEachLoadedPlayerFile((playerName, playerFile) ->
    {
      SocialSpyLevel level = playerFile.getSocialSpyLevel();

      if (level == SocialSpyLevel.ALL || (level == SocialSpyLevel.SERVER && isFromCurrentServer))
      {
        Player player = server.getPlayerExact(playerName);

        if (player != null)
        {
          player.sendMessage(socialSpyMessage);
        }
      }
    });
  }
}

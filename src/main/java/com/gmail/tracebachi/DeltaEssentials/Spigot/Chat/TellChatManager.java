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
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ResponseMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TellChatManager implements Registerable
{
  private static final long CONSUMER_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

  private final DeltaEssentialsPlugin plugin;
  private final SocialSpyListener socialSpyListener;
  private final MessageFormatMap formatMap;
  private final BasicLogger basicLogger;
  private final SockExchangeApi api;
  private final Consumer<ReceivedMessage> tellChannelListener;

  public TellChatManager(
    DeltaEssentialsPlugin plugin, SocialSpyListener socialSpyListener, MessageFormatMap formatMap,
    BasicLogger basicLogger, SockExchangeApi api)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(socialSpyListener, "socialSpyListener");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(basicLogger, "basicLogger");
    Preconditions.checkNotNull(api, "api");

    this.plugin = plugin;
    this.socialSpyListener = socialSpyListener;
    this.formatMap = formatMap;
    this.basicLogger = basicLogger;
    this.api = api;
    this.tellChannelListener = this::onTellChannelRequest;
  }

  @Override
  public void register()
  {
    api.getMessageNotifier().register(Channels.TELL, tellChannelListener);
  }

  @Override
  public void unregister()
  {
    api.getMessageNotifier().unregister(Channels.TELL, tellChannelListener);
  }

  public void sendMessage(
    String senderName, String receiverName, String message, boolean ignoreVanish)
  {
    ConcurrentHashMap<String, byte[]> extraDataMap = new ConcurrentHashMap<>(2);
    Consumer<TellSendEvent> tellSendEventConsumer = (e) ->
    {
      plugin.executeSync(() -> onTellSendEventComplete(e));
    };

    TellSendEvent tellSendEvent = new TellSendEvent(
      senderName, receiverName, ignoreVanish, extraDataMap, message, tellSendEventConsumer);

    plugin.callDelayedHandlingEvent(tellSendEvent);
  }

  private void onTellChannelRequest(ReceivedMessage receivedMessage)
  {
    // Respond to confirm message was received
    receivedMessage.respond();

    ByteArrayDataInput in = receivedMessage.getDataInput();
    String sourceServerName = in.readUTF();
    String senderName = in.readUTF();
    String receiverName = in.readUTF();
    String message = in.readUTF();
    boolean ignoreVanish = in.readBoolean();

    ConcurrentHashMap<String, byte[]> extraDataMap = readStringByteArrayMap(in);
    Consumer<TellReceiveEvent> tellReceiveEventConsumer = (e) ->
    {
      plugin.executeSync(() -> onTellReceiveEventComplete(sourceServerName, e));
    };

    TellReceiveEvent event = new TellReceiveEvent(senderName, receiverName,
      ignoreVanish, extraDataMap, message, tellReceiveEventConsumer);

    // Call event sync
    plugin.executeSync(() -> plugin.callDelayedHandlingEvent(event));
  }

  private void onTellSendEventComplete(TellSendEvent event)
  {
    String receiverName = event.getReceiverName();
    String senderName = event.getSenderName();
    boolean ignoreVanish = event.canIgnoreVanish();
    String message = event.getMessage();
    String currentServerName = api.getServerName();

    if (event.isCancelled())
    {
      String cancelMessage = event.getCancelMessage();

      if (cancelMessage != null)
      {
        sendChatMessage(cancelMessage, senderName, currentServerName);
      }

      return;
    }

    Player receiver = plugin.getServer().getPlayerExact(receiverName);

    // Check if the receiver is on the current server
    if (receiver != null)
    {
      Consumer<TellReceiveEvent> tellReceiveEventConsumer = (e) ->
      {
        plugin.executeSync(() -> onTellReceiveEventComplete(currentServerName, e));
      };

      TellReceiveEvent tellReceiveEvent = new TellReceiveEvent(senderName, receiverName,
        ignoreVanish, event.getExtraDataMap(), message, tellReceiveEventConsumer);

      plugin.callDelayedHandlingEvent(tellReceiveEvent);
      return;
    }

    ByteArrayDataOutput out = ByteStreams.newDataOutput(384);
    out.writeUTF(currentServerName);
    out.writeUTF(senderName);
    out.writeUTF(receiverName);
    out.writeUTF(message);
    out.writeBoolean(ignoreVanish);
    writeStringByteArrayMap(out, event.getExtraDataMap());

    api.sendToServerOfPlayer(
      Channels.TELL, out.toByteArray(), receiverName,
      (rm) -> onAnyResponseCheckIfOk(senderName, receiverName, rm), CONSUMER_TIMEOUT);
  }

  private void onTellReceiveEventComplete(String sourceServerName, TellReceiveEvent event)
  {
    String senderName = event.getSenderName();
    String receiverName = event.getReceiverName();

    if (event.isCancelled())
    {
      String cancelMessage = event.getCancelMessage();

      if (cancelMessage != null)
      {
        sendChatMessage(cancelMessage, senderName, sourceServerName);
      }

      return;
    }

    Player player = plugin.getServer().getPlayerExact(receiverName);

    if (player == null)
    {
      String message = formatMap.format(FormatNames.PLAYER_OFFLINE, receiverName);
      sendChatMessage(message, senderName, sourceServerName);
      return;
    }

    boolean ignoreVanish = event.canIgnoreVanish();
    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(receiverName);

    if (playerFile != null)
    {
      if (!ignoreVanish && playerFile.isVanished())
      {
        String message = formatMap.format(FormatNames.PLAYER_OFFLINE, receiverName);
        sendChatMessage(message, senderName, sourceServerName);
        return;
      }

      playerFile.setReplyingTo(senderName);
    }

    // Log the message
    String message = event.getMessage();
    basicLogger.info(formatMap.format(FormatNames.TELL_LOG, senderName, receiverName, message));

    // Send the message to the sender
    String messageForSender = formatMap.format(FormatNames.TELL_SENDER, senderName, receiverName,
      message);
    sendChatMessage(messageForSender, senderName, sourceServerName);

    // Send the message to the receiver
    String messageForReceiver = formatMap.format(FormatNames.TELL_RECEIVER, senderName,
      receiverName, message);
    sendChatMessage(messageForReceiver, receiverName, api.getServerName());

    // Send the message to social spies
    socialSpyListener.sendMessageToSocialSpies(senderName, receiverName, message);
  }

  private void onAnyResponseCheckIfOk(
    String senderName, String receiverName, ResponseMessage responseMessage)
  {
    if (!responseMessage.getResponseStatus().isOk())
    {
      String message = formatMap.format(FormatNames.PLAYER_OFFLINE, receiverName);
      sendChatMessage(message, senderName, api.getServerName());
    }
  }

  private void sendChatMessage(String message, String receiverName, String sourceServerName)
  {
    api.sendChatMessages(Collections.singletonList(message), receiverName, sourceServerName);
  }

  private void writeStringByteArrayMap(ByteArrayDataOutput out, Map<String, byte[]> map)
  {
    int entryCount = 0;

    for (Map.Entry<String, byte[]> entry : map.entrySet())
    {
      String entryKey = entry.getKey();
      byte[] entryValue = entry.getValue();

      if (entryKey != null && !entryKey.isEmpty() && entryValue != null)
      {
        entryCount++;
      }
    }

    // Write the number of entries
    out.writeInt(entryCount);

    for (Map.Entry<String, byte[]> entry : map.entrySet())
    {
      String entryKey = entry.getKey();
      byte[] entryValue = entry.getValue();

      if (entryKey != null && !entryKey.isEmpty() && entryValue != null)
      {
        // Write the key, number of bytes, and the bytes
        out.writeUTF(entryKey);
        out.writeInt(entryValue.length);
        out.write(entryValue);
      }
    }
  }

  private ConcurrentHashMap<String, byte[]> readStringByteArrayMap(ByteArrayDataInput in)
  {
    int entryCount = in.readInt();
    ConcurrentHashMap<String, byte[]> map = new ConcurrentHashMap<>();

    for (int i = 0; i < entryCount; i++)
    {
      String entryKey = in.readUTF();
      int numBytes = in.readInt();
      byte[] bytes = new byte[numBytes];

      // Read the bytes
      in.readFully(bytes);

      // Save to the map
      map.put(entryKey, bytes);
    }

    return map;
  }
}

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

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Channels;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.FormatNames;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Permissions;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.Teleport.PlayerTpEvent.TeleportType;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ResponseMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.SpigotServerInfo;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TpToManager implements Listener, Registerable
{
  private static final long CONSUMER_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

  private final DeltaEssentialsPlugin plugin;
  private final TpAfterLoadListener tpAfterLoadListener;
  private final SameServerTeleporter sameServerTeleporter;
  private final MessageFormatMap formatMap;
  private final SockExchangeApi api;
  private final Consumer<ReceivedMessage> tpChannelListener;

  public TpToManager(
    DeltaEssentialsPlugin plugin, TpAfterLoadListener tpAfterLoadListener,
    SameServerTeleporter sameServerTeleporter, MessageFormatMap formatMap, SockExchangeApi api)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(tpAfterLoadListener, "tpAfterLoadListener");
    Preconditions.checkNotNull(sameServerTeleporter, "sameServerTeleporter");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(api, "api");

    this.plugin = plugin;
    this.tpAfterLoadListener = tpAfterLoadListener;
    this.sameServerTeleporter = sameServerTeleporter;
    this.formatMap = formatMap;
    this.api = api;
    this.tpChannelListener = this::onTpChannelRequest;
  }

  @Override
  public void register()
  {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);

    api.getMessageNotifier().register(Channels.TP_TO, tpChannelListener);
  }

  @Override
  public void unregister()
  {
    api.getMessageNotifier().unregister(Channels.TP_TO, tpChannelListener);

    HandlerList.unregisterAll(this);
  }

  public void tpOtherToOther(Player startPlayer, String destPlayerName)
  {
    internalTpTo(startPlayer, destPlayerName, true, true, true);
  }

  public void tpTo(
    Player startPlayer, String destPlayerName, boolean ignoreBlocking, boolean ignoreVanish)
  {
    internalTpTo(startPlayer, destPlayerName, false, ignoreBlocking, ignoreVanish);
  }

  private void internalTpTo(
    Player startPlayer, String destPlayerName, boolean isTpOtherToOther, boolean ignoreBlocking,
    boolean ignoreVanish)
  {
    String startPlayerName = startPlayer.getName();
    Player destPlayer = plugin.getServer().getPlayerExact(destPlayerName);

    // Check if both players are on the same server
    if (destPlayer != null)
    {
      onSameServerTpTo(startPlayer, destPlayer, isTpOtherToOther, ignoreBlocking, ignoreVanish);
      return;
    }

    // If one of the players is not on the same server, a request must be made.
    ByteArrayDataOutput out = ByteStreams.newDataOutput(128);
    out.writeUTF(startPlayerName);
    out.writeUTF(destPlayerName);
    out.writeBoolean(isTpOtherToOther);
    out.writeBoolean(ignoreBlocking);
    out.writeBoolean(ignoreVanish);
    writeStringList(out, getAccessiblePrivateServerNames(startPlayer));

    api.sendToServerOfPlayer(Channels.TP_TO, out.toByteArray(), destPlayerName,
      (m) -> onAnyResponseCheckIfOk(startPlayerName, destPlayerName, m), CONSUMER_TIMEOUT);
  }

  private void onAnyResponseCheckIfOk(
    String senderName, String otherPlayerName, ResponseMessage responseMessage)
  {
    if (!responseMessage.getResponseStatus().isOk())
    {
      String message = formatMap.format(FormatNames.PLAYER_OFFLINE, otherPlayerName);
      api.sendChatMessages(Collections.singletonList(message), senderName, null);
    }
  }

  private void onTpChannelRequest(ReceivedMessage message)
  {
    // Respond to confirm message was received
    message.respond();

    ByteArrayDataInput in = message.getDataInput();
    String startPlayerName = in.readUTF();
    String destPlayerName = in.readUTF();
    boolean isTpOtherToOther = in.readBoolean();
    boolean ignoreBlocking = in.readBoolean();
    boolean ignoreVanish = in.readBoolean();
    List<String> accessibleServerNames = readStringList(in);

    plugin.executeSync(() ->
    {
      onDiffServerTpTo(
        startPlayerName, destPlayerName, isTpOtherToOther, ignoreBlocking, ignoreVanish,
        accessibleServerNames);
    });
  }

  private void onSameServerTpTo(
    Player startPlayer, Player destPlayer, boolean isTpOtherToOther, boolean ignoreBlocking,
    boolean ignoreVanish)
  {
    String message;
    String startPlayerName = startPlayer.getName();
    String destPlayerName = destPlayer.getName();

    if (isTpOtherToOther)
    {
      sameServerTeleporter.teleport(startPlayer, destPlayer, TeleportType.TP_OTHER_TO_OTHER);
      return;
    }

    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(destPlayerName);

    if (playerFile != null)
    {
      if (!ignoreBlocking && playerFile.isBlockingTeleports())
      {
        message = formatMap.format(FormatNames.BLOCKING_TELEPORTS, destPlayerName);
        api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
        return;
      }

      if (!ignoreVanish && playerFile.isVanished())
      {
        message = formatMap.format(FormatNames.PLAYER_OFFLINE, destPlayerName);
        api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
        return;
      }
    }

    sameServerTeleporter.teleport(startPlayer, destPlayer, TeleportType.TP_TO);
  }

  private void onDiffServerTpTo(
    String startPlayerName, String destPlayerName, boolean isTpOtherToOther, boolean ignoreBlocking,
    boolean ignoreVanish, List<String> accessibleServerNames)
  {
    String message;
    Player destPlayer = plugin.getServer().getPlayerExact(destPlayerName);

    if (destPlayer == null)
    {
      message = formatMap.format(FormatNames.PLAYER_OFFLINE, destPlayerName);
      api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
      return;
    }

    String currentServerName = api.getServerName();

    if (isTpOtherToOther)
    {
      tpAfterLoadListener.save(startPlayerName, destPlayerName, TeleportType.TP_OTHER_TO_OTHER);
      api.movePlayers(Collections.singleton(startPlayerName), currentServerName);
      return;
    }

    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(destPlayerName);

    if (playerFile != null)
    {
      if (!ignoreBlocking && playerFile.isBlockingTeleports())
      {
        message = formatMap.format(FormatNames.BLOCKING_TELEPORTS, destPlayerName);
        api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
        return;
      }

      if (!ignoreVanish && playerFile.isVanished())
      {
        message = formatMap.format(FormatNames.PLAYER_OFFLINE, destPlayerName);
        api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
        return;
      }
    }

    SpigotServerInfo currentServerInfo = api.getServerInfo(currentServerName);

    if (currentServerInfo != null && currentServerInfo.isPrivate())
    {
      if (!accessibleServerNames.contains(currentServerName))
      {
        String permission = Permissions.TP_TO_PRIVATE_SERVER_PERM_PREFIX + currentServerName;

        message = formatMap.format(FormatNames.NO_PERM, permission);
        api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
        return;
      }
    }

    tpAfterLoadListener.save(startPlayerName, destPlayerName, TeleportType.TP_TO);
    api.movePlayers(Collections.singleton(startPlayerName), currentServerName);
  }

  private List<String> getAccessiblePrivateServerNames(Player startPlayer)
  {
    List<String> usablePrivateServers = new ArrayList<>();
    for (SpigotServerInfo serverInfo : api.getServerInfos())
    {
      String serverName = serverInfo.getServerName();
      String permission = Permissions.TP_TO_PRIVATE_SERVER_PERM_PREFIX + serverName;

      if (serverInfo.isPrivate() && startPlayer.hasPermission(permission))
      {
        usablePrivateServers.add(serverName);
      }
    }
    return usablePrivateServers;
  }

  private void writeStringList(ByteArrayDataOutput out, List<String> stringList)
  {
    out.writeInt(stringList.size());

    for (String str : stringList)
    {
      Preconditions.checkNotNull(str, "str");
      out.writeUTF(str);
    }
  }

  private List<String> readStringList(ByteArrayDataInput in)
  {
    int count = in.readInt();
    List<String> stringList = new ArrayList<>(count);

    for (int i = 0; i < count; i++)
    {
      stringList.add(in.readUTF());
    }

    return stringList;
  }
}

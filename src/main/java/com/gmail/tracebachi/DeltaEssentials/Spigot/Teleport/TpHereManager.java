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
import com.gmail.tracebachi.SockExchange.Utilities.CaseInsensitiveMap;
import com.gmail.tracebachi.SockExchange.Utilities.ExtraPreconditions;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TpHereManager implements Listener, Registerable
{
  private static final long CONSUMER_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
  private static final long TP_ASK_HERE_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(15);
  private static final long CLEANUP_REQUEST_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(20);

  private final DeltaEssentialsPlugin plugin;
  private final SameServerTeleporter sameServerTeleporter;
  private final MessageFormatMap formatMap;
  private final SockExchangeApi api;
  private final CaseInsensitiveMap<TpAskHereRequest> tpAskHereRequestMap;
  private final Consumer<ReceivedMessage> tpHereChannelListener;
  private ScheduledFuture<?> cleanupFuture;

  public TpHereManager(
    DeltaEssentialsPlugin plugin, SameServerTeleporter sameServerTeleporter,
    MessageFormatMap formatMap, SockExchangeApi api)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(sameServerTeleporter, "sameServerTeleporter");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(api, "api");

    this.plugin = plugin;
    this.sameServerTeleporter = sameServerTeleporter;
    this.formatMap = formatMap;
    this.api = api;
    this.tpAskHereRequestMap = new CaseInsensitiveMap<>(new HashMap<>());
    this.tpHereChannelListener = this::onTpHereChannelRequest;
  }

  @Override
  public void register()
  {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);

    api.getMessageNotifier().register(Channels.TP_HERE, tpHereChannelListener);

    cleanupFuture = api.getScheduledExecutorService().scheduleAtFixedRate(
      () -> plugin.executeSync(this::cleanupRequests), CLEANUP_REQUEST_PERIOD_MILLIS,
      CLEANUP_REQUEST_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
  }

  @Override
  public void unregister()
  {
    if (cleanupFuture != null)
    {
      cleanupFuture.cancel(false);
      cleanupFuture = null;
    }

    api.getMessageNotifier().unregister(Channels.TP_HERE, tpHereChannelListener);

    HandlerList.unregisterAll(this);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerQuit(PlayerQuitEvent event)
  {
    String playerName = event.getPlayer().getName();
    tpAskHereRequestMap.remove(playerName);
  }

  public void tpHere(String startPlayerName, Player destPlayer)
  {
    internalTpHere(startPlayerName, destPlayer, false, true, true);
  }

  public void tpAskHere(
    String startPlayerName, Player destPlayer, boolean ignoreBlocking, boolean ignoreVanish)
  {
    internalTpHere(startPlayerName, destPlayer, true, ignoreBlocking, ignoreVanish);
  }

  private void internalTpHere(
    String startPlayerName, Player destPlayer, boolean isAsking, boolean ignoreBlocking,
    boolean ignoreVanish)
  {
    String destPlayerName = destPlayer.getName();
    Player startPlayer = plugin.getServer().getPlayerExact(startPlayerName);

    // Check if both players are on the same server
    if (startPlayer != null)
    {
      onSameServerTpHere(startPlayer, destPlayer, isAsking, ignoreBlocking, ignoreVanish);
      return;
    }

    // If one of the players is not on the same server, a request must be made.
    ByteArrayDataOutput out = ByteStreams.newDataOutput(200);
    out.writeUTF(startPlayerName);
    out.writeUTF(destPlayerName);
    out.writeUTF(api.getServerName());
    out.writeBoolean(isAsking);
    out.writeBoolean(ignoreBlocking);
    out.writeBoolean(ignoreVanish);

    api.sendToServerOfPlayer(Channels.TP_HERE, out.toByteArray(), startPlayerName,
      (m) -> onAnyResponseCheckIfOk(destPlayerName, startPlayerName, m), CONSUMER_TIMEOUT);
  }

  public void acceptTpAskHere(Player startPlayer)
  {
    String startPlayerName = startPlayer.getName();
    TpAskHereRequest tpAskHereRequest = tpAskHereRequestMap.remove(startPlayerName);

    if (tpAskHereRequest == null)
    {
      // Respond to the acceptor that they have no requests
      String message = formatMap.format(FormatNames.TP_ASK_HERE_NO_REQUESTS);
      api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
      return;
    }

    String destPlayerName = tpAskHereRequest.getDestPlayerName();
    String destServerName = tpAskHereRequest.getDestServerName();
    String currentServerName = api.getServerName();

    if (!destServerName.equalsIgnoreCase(currentServerName))
    {
      // Request the destServer to teleport when the player is loaded
      sendRequestToTpAfterLoad(
        startPlayerName, destPlayerName, destServerName, TeleportType.TP_ASK_HERE);

      // Move player to destServer
      api.movePlayers(Collections.singleton(startPlayerName), destServerName);
      return;
    }

    Player destPlayer = plugin.getServer().getPlayerExact(destPlayerName);

    if (destPlayer == null)
    {
      String message = formatMap.format(FormatNames.PLAYER_OFFLINE, destPlayerName);
      api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
      return;
    }

    sameServerTeleporter.teleport(startPlayer, destPlayer, TeleportType.TP_ASK_HERE);
  }

  public void denyTpAskHere(Player startPlayer)
  {
    String startPlayerName = startPlayer.getName();
    TpAskHereRequest tpAskHereRequest = tpAskHereRequestMap.remove(startPlayerName);
    String message;

    if (tpAskHereRequest == null)
    {
      // Respond to the denier that they have no requests
      message = formatMap.format(FormatNames.TP_ASK_HERE_NO_REQUESTS);
      api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
      return;
    }

    String destPlayerName = tpAskHereRequest.getDestPlayerName();

    // Respond to the sender and denier that the request was denied
    message = formatMap.format(FormatNames.TP_ASK_HERE_DENIED, startPlayerName, destPlayerName);
    api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
    api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);
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

  private void onTpHereChannelRequest(ReceivedMessage message)
  {
    // Respond to confirm message was received
    message.respond();

    ByteArrayDataInput in = message.getDataInput();
    String startPlayerName = in.readUTF();
    String destPlayerName = in.readUTF();
    String destServerName = in.readUTF();
    boolean isAsking = in.readBoolean();
    boolean ignoreBlocking = in.readBoolean();
    boolean ignoreVanish = in.readBoolean();

    plugin.executeSync(() ->
    {
      onDiffServersTpHere(
        startPlayerName, destPlayerName, destServerName, isAsking, ignoreBlocking, ignoreVanish);
    });
  }

  private void onSameServerTpHere(
    Player startPlayer, Player destPlayer, boolean isAsking, boolean ignoreBlocking,
    boolean ignoreVanish)
  {
    if (!isAsking)
    {
      sameServerTeleporter.teleport(startPlayer, destPlayer, TeleportType.TP_HERE);
      return;
    }

    String message;
    String startPlayerName = startPlayer.getName();
    String destPlayerName = destPlayer.getName();
    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(startPlayerName);

    if (playerFile != null)
    {
      if (!ignoreBlocking && playerFile.isBlockingTeleports())
      {
        message = formatMap.format(FormatNames.BLOCKING_TELEPORTS, startPlayerName);
        api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);
        return;
      }

      if (!ignoreVanish && playerFile.isVanished())
      {
        message = formatMap.format(FormatNames.PLAYER_OFFLINE, startPlayerName);
        api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);
        return;
      }
    }

    String destServerName = api.getServerName();

    saveTpAskHereRequest(startPlayerName, destPlayerName, destServerName);
    tellPlayersAboutTpAskHereRequest(startPlayerName, destPlayerName);
  }

  private void onDiffServersTpHere(
    String startPlayerName, String destPlayerName, String destServerName, boolean isAsking,
    boolean ignoreBlocking, boolean ignoreVanish)
  {
    String message;
    Player startPlayer = plugin.getServer().getPlayerExact(startPlayerName);

    if (startPlayer == null)
    {
      message = formatMap.format(FormatNames.PLAYER_OFFLINE, startPlayerName);
      api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);
      return;
    }

    if (!isAsking)
    {
      // Request destServer to teleport when the player is loaded
      sendRequestToTpAfterLoad(startPlayerName, destPlayerName, destServerName, TeleportType.TP_HERE);

      // Move the player to destServer
      api.movePlayers(Collections.singleton(startPlayerName), destServerName);
      return;
    }

    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(startPlayerName);

    if (playerFile != null)
    {
      if (!ignoreBlocking && playerFile.isBlockingTeleports())
      {
        message = formatMap.format(FormatNames.BLOCKING_TELEPORTS, startPlayerName);
        api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);
        return;
      }

      if (!ignoreVanish && playerFile.isVanished())
      {
        message = formatMap.format(FormatNames.PLAYER_OFFLINE, startPlayerName);
        api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);
        return;
      }
    }

    SpigotServerInfo serverInfo = api.getServerInfo(destServerName);
    String permission = Permissions.TP_TO_PRIVATE_SERVER_PERM_PREFIX + destPlayerName;

    if (serverInfo != null && serverInfo.isPrivate() && !startPlayer.hasPermission(permission))
    {
      message = formatMap.format(FormatNames.OTHER_CANT_JOIN_PRIVATE_SERVER, startPlayerName);
      api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);
      return;
    }

    saveTpAskHereRequest(startPlayerName, destPlayerName, destServerName);
    tellPlayersAboutTpAskHereRequest(startPlayerName, destPlayerName);
  }

  private void sendRequestToTpAfterLoad(
    String startPlayerName, String destPlayerName, String destServerName, TeleportType teleportType)
  {
    int estimatedSize = (startPlayerName.length() + destPlayerName.length()) * 2;
    ByteArrayDataOutput out = ByteStreams.newDataOutput(estimatedSize);
    out.writeUTF(startPlayerName);
    out.writeUTF(destPlayerName);
    out.writeByte(teleportType.ordinal());

    // Send a message for dest server to create a TpAfterLoadRequest for nameToTp
    api.sendToServer(Channels.TP_AFTER_LOAD, out.toByteArray(), destServerName);
  }

  private void saveTpAskHereRequest(
    String startPlayerName, String destPlayerName, String destServerName)
  {
    TpAskHereRequest tpAskHereRequest = new TpAskHereRequest(destPlayerName, destServerName,
      System.currentTimeMillis() + TP_ASK_HERE_REQUEST_TIMEOUT);

    tpAskHereRequestMap.put(startPlayerName, tpAskHereRequest);
  }

  private void tellPlayersAboutTpAskHereRequest(String startPlayerName, String destPlayerName)
  {
    String message;

    message = formatMap.format(FormatNames.TP_ASK_HERE_SENT, startPlayerName);
    api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);

    message = formatMap.format(FormatNames.TP_ASK_HERE_RECEIVED, destPlayerName);
    api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
  }

  private void cleanupRequests()
  {
    long currentTimeMillis = System.currentTimeMillis();

    tpAskHereRequestMap.values().removeIf(
      request -> currentTimeMillis > request.getExpiresAtMillis());
  }

  private static class TpAskHereRequest
  {
    private final String destPlayerName;
    private final String destServerName;
    private final long expiresAtMillis;

    TpAskHereRequest(String destPlayerName, String destServerName, long expiresAtMillis)
    {
      ExtraPreconditions.checkNotEmpty(destPlayerName, "destPlayerName");
      ExtraPreconditions.checkNotEmpty(destServerName, "destServerName");

      this.destPlayerName = destPlayerName;
      this.destServerName = destServerName;
      this.expiresAtMillis = expiresAtMillis;
    }

    String getDestPlayerName()
    {
      return destPlayerName;
    }

    String getDestServerName()
    {
      return destServerName;
    }

    long getExpiresAtMillis()
    {
      return expiresAtMillis;
    }
  }
}

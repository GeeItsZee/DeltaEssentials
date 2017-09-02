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
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving.PlayerPostLoadEvent;
import com.gmail.tracebachi.DeltaEssentials.Spigot.Teleport.PlayerTpEvent.TeleportType;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.CaseInsensitiveMap;
import com.gmail.tracebachi.SockExchange.Utilities.ExtraPreconditions;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
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
public class TpAfterLoadListener implements Listener, Registerable
{
  private static final long TP_AFTER_LOAD_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
  private static final long CLEANUP_REQUEST_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(20);

  private final DeltaEssentialsPlugin plugin;
  private final SameServerTeleporter sameServerTeleporter;
  private final MessageFormatMap formatMap;
  private final SockExchangeApi api;
  private final CaseInsensitiveMap<TpAfterLoadRequest> tpAfterLoadRequestMap;
  private final Consumer<ReceivedMessage> tpAfterLoadChannelListener;
  private ScheduledFuture<?> cleanupFuture;

  public TpAfterLoadListener(
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
    this.tpAfterLoadRequestMap = new CaseInsensitiveMap<>(new HashMap<>());
    this.tpAfterLoadChannelListener = this::onTpAfterLoadChannelRequest;
  }

  @Override
  public void register()
  {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);

    api.getMessageNotifier().register(Channels.TP_AFTER_LOAD, tpAfterLoadChannelListener);

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

    api.getMessageNotifier().unregister(Channels.TP_AFTER_LOAD, tpAfterLoadChannelListener);

    HandlerList.unregisterAll(this);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerLoad(PlayerPostLoadEvent event)
  {
    Player playerToTp = event.getPlayer();
    String nameToTp = playerToTp.getName();
    TpAfterLoadRequest request = tpAfterLoadRequestMap.remove(nameToTp);

    if (request != null)
    {
      String destPlayerName = request.getDestPlayerName();
      TeleportType teleportType = request.getTeleportType();

      teleportIfDestPlayerOnline(playerToTp, nameToTp, destPlayerName, teleportType);
    }
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerQuit(PlayerQuitEvent event)
  {
    String playerName = event.getPlayer().getName();
    tpAfterLoadRequestMap.remove(playerName);
  }

  public void save(
    String startPlayerName, String destPlayerName, TeleportType teleportType)
  {
    long expiresAtMillis = System.currentTimeMillis() + TP_AFTER_LOAD_REQUEST_TIMEOUT;
    TpAfterLoadRequest tpRequest = new TpAfterLoadRequest(
      destPlayerName, teleportType, expiresAtMillis);

    tpAfterLoadRequestMap.put(startPlayerName, tpRequest);
  }

  private void onTpAfterLoadChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String startPlayerName = in.readUTF();
    String destPlayerName = in.readUTF();
    TeleportType teleportType = TeleportType.fromOrdinal(in.readByte());

    plugin.executeSync(() ->
    {
      Player startPlayer = plugin.getServer().getPlayerExact(startPlayerName);

      if (startPlayer != null)
      {
        teleportIfDestPlayerOnline(startPlayer, startPlayerName, destPlayerName, teleportType);
        return;
      }

      save(startPlayerName, destPlayerName, teleportType);
    });
  }

  private void teleportIfDestPlayerOnline(
    Player startPlayer, String startPlayerName, String destPlayerName, TeleportType teleportType)
  {
    Player destPlayer = plugin.getServer().getPlayerExact(destPlayerName);

    // If the destPlayer is not online, let the startPlayer know.
    if (destPlayer == null)
    {
      String message = formatMap.format(FormatNames.PLAYER_OFFLINE, destPlayerName);
      api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
      return;
    }

    sameServerTeleporter.teleport(startPlayer, destPlayer, teleportType);
  }

  private void cleanupRequests()
  {
    long currentTimeMillis = System.currentTimeMillis();

    tpAfterLoadRequestMap.values().removeIf(
      request -> currentTimeMillis > request.getExpiresAtMillis());
  }

  public static class TpAfterLoadRequest
  {
    private final String destPlayerName;
    private final TeleportType teleportType;
    private final long expiresAtMillis;

    TpAfterLoadRequest(String destPlayerName, TeleportType teleportType, long expiresAtMillis)
    {
      ExtraPreconditions.checkNotEmpty(destPlayerName, "destPlayerName");

      this.destPlayerName = destPlayerName;
      this.teleportType = teleportType;
      this.expiresAtMillis = expiresAtMillis;
    }

    String getDestPlayerName()
    {
      return destPlayerName;
    }

    public TeleportType getTeleportType()
    {
      return teleportType;
    }

    long getExpiresAtMillis()
    {
      return expiresAtMillis;
    }
  }
}

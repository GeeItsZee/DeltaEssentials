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
import com.gmail.tracebachi.DeltaEssentials.Spigot.Teleport.PlayerTpEvent.TeleportType;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collections;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class SameServerTeleporter
{
  private static final String SILENT_TP_PERM = "DeltaEss.Tp.Silent";

  private final DeltaEssentialsPlugin plugin;
  private final MessageFormatMap formatMap;
  private final SockExchangeApi api;

  public SameServerTeleporter(
    DeltaEssentialsPlugin plugin, MessageFormatMap formatMap, SockExchangeApi api)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(api, "api");

    this.plugin = plugin;
    this.formatMap = formatMap;
    this.api = api;
  }

  public void teleport(Player startPlayer, Player destPlayer, TeleportType teleportType)
  {
    Location location = destPlayer.getLocation();
    String startPlayerName = startPlayer.getName();
    String destPlayerName = destPlayer.getName();
    String message;

    if (!startPlayer.isFlying() && startPlayer.getGameMode() != GameMode.CREATIVE)
    {
      location = getSafeLocation(location);

      if (location == null)
      {
        message = formatMap.format(FormatNames.UNSAFE_TP_LOCATION, destPlayerName);
        api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
        return;
      }
    }

    PlayerTpEvent event = new PlayerTpEvent(startPlayer, destPlayer, location, teleportType);
    plugin.callEvent(event);

    if (event.isCancelled())
    {
      message = event.getCancelMessage();

      if (message != null)
      {
        api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);
      }

      return;
    }

    // Update the location in case it was changed
    location = event.getLocation();

    message = formatMap.format(FormatNames.TELEPORTING_TO, destPlayerName);
    api.sendChatMessages(Collections.singletonList(message), startPlayerName, null);

    // Do the teleport
    startPlayer.teleport(location, PlayerTeleportEvent.TeleportCause.COMMAND);

    // Teleports are only silent if start has the perm, but dest does not
    if (!startPlayer.hasPermission(SILENT_TP_PERM) || destPlayer.hasPermission(SILENT_TP_PERM))
    {
      message = formatMap.format(FormatNames.TELEPORTED_TO_YOU, startPlayerName);
      api.sendChatMessages(Collections.singletonList(message), destPlayerName, null);
    }
  }

  private Location getSafeLocation(Location start)
  {
    if (start == null)
    {
      return null;
    }

    World world = start.getWorld();

    if (world == null)
    {
      return null;
    }

    int startX = start.getBlockX();
    int startY = start.getBlockY();
    int startZ = start.getBlockZ();
    int iterY = startY;

    while (iterY > 0)
    {
      Block block = world.getBlockAt(startX, iterY, startZ);

      if (!block.isEmpty())
      {
        switch (block.getType())
        {
          case LAVA:
          case STATIONARY_LAVA:
          case FIRE:
            return null;
          default:
            return block.getLocation().add(0.5, 1.0, 0.5);
        }
      }

      iterY--;
    }

    return null;
  }
}

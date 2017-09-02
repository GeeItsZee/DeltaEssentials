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

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerTpEvent extends Event
{
  private final Player startPlayer;
  private final Player destPlayer;
  private final TeleportType teleportType;
  private Location location;
  private boolean cancelled;
  private String cancelMessage;

  public PlayerTpEvent(
    Player startPlayer, Player destPlayer, Location location, TeleportType teleportType)
  {
    Preconditions.checkNotNull(startPlayer, "startPlayer");
    Preconditions.checkNotNull(destPlayer, "destPlayer");
    Preconditions.checkNotNull(location, "location");
    Preconditions.checkNotNull(teleportType, "teleportType");

    this.startPlayer = startPlayer;
    this.destPlayer = destPlayer;
    this.location = location;
    this.teleportType = teleportType;
  }

  public Player getStartPlayer()
  {
    return startPlayer;
  }

  public Player getDestPlayer()
  {
    return destPlayer;
  }

  public Location getLocation()
  {
    return location;
  }

  public TeleportType getTeleportType()
  {
    return teleportType;
  }

  public void setLocation(Location location)
  {
    Preconditions.checkNotNull(location, "location");

    this.location = location;
  }

  public boolean isCancelled()
  {
    return cancelled;
  }

  public void setCancelled(boolean cancelled)
  {
    this.cancelled = cancelled;
  }

  public String getCancelMessage()
  {
    return cancelMessage;
  }

  public void setCancelMessage(String cancelMessage)
  {
    this.cancelMessage = cancelMessage;
  }

  /* START Required by Spigot *********************************************************************/

  private static final HandlerList handlers = new HandlerList();

  public HandlerList getHandlers()
  {
    return handlers;
  }

  public static HandlerList getHandlerList()
  {
    return handlers;
  }

  /* END Required by Spigot ***********************************************************************/

  public enum TeleportType
  {
    TP_TO,
    TP_OTHER_TO_OTHER,
    TP_HERE,
    TP_ASK_HERE;

    public static TeleportType fromOrdinal(int ordinal)
    {
      switch (ordinal)
      {
        case 0:
          return TP_TO;
        case 1:
          return TP_OTHER_TO_OTHER;
        case 2:
          return TP_HERE;
        case 3:
          return TP_ASK_HERE;
        default:
          throw new IllegalArgumentException("Unknown mapping for ordinal");
      }
    }
  }
}

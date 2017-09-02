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
package com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileEditing;

import com.gmail.tracebachi.SockExchange.Utilities.ExtraPreconditions;
import com.google.common.base.Preconditions;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
class PlayerFileInventoryHolder implements InventoryHolder
{
  enum Type
  {
    SURVIVAL,
    CREATIVE,
    ENDER_CHEST
  }

  private final String playerFileOwner;
  private final Type type;

  public PlayerFileInventoryHolder(
    String playerFileOwner, Type type)
  {
    this.playerFileOwner = ExtraPreconditions.checkNotEmpty(playerFileOwner, "playerFileOwner");
    this.type = Preconditions.checkNotNull(type, "type");
  }

  @Override
  public Inventory getInventory()
  {
    return null;
  }

  public String getPlayerFileOwner()
  {
    return playerFileOwner;
  }

  public Type getType()
  {
    return type;
  }
}

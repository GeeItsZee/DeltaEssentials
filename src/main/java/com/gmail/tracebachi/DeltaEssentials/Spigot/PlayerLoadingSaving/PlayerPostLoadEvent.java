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
package com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving;

import com.google.common.base.Preconditions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author GeeItsZee (tracebachi@gmail.com).
 */
public class PlayerPostLoadEvent extends Event
{
  private final Player player;
  private final ConfigurationSection pluginPlayerData;
  private final boolean firstJoin;

  public PlayerPostLoadEvent(Player player, ConfigurationSection pluginPlayerData)
  {
    this(player, pluginPlayerData, false);
  }

  public PlayerPostLoadEvent(
    Player player, ConfigurationSection pluginPlayerData, boolean firstJoin)
  {
    Preconditions.checkNotNull(player, "player");
    Preconditions.checkNotNull(pluginPlayerData, "pluginPlayerData");

    this.player = player;
    this.pluginPlayerData = pluginPlayerData;
    this.firstJoin = firstJoin;
  }

  public Player getPlayer()
  {
    return player;
  }

  public ConfigurationSection getPluginPlayerData()
  {
    return pluginPlayerData;
  }

  public boolean isFirstJoin()
  {
    return firstJoin;
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
}

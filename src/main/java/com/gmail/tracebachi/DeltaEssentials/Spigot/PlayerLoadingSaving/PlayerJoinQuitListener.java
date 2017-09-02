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

import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerJoinQuitListener implements Listener, Registerable
{
  private final DeltaEssentialsPlugin plugin;
  private final PlayerLoaderSaver playerLoaderSaver;

  public PlayerJoinQuitListener(
    DeltaEssentialsPlugin plugin, PlayerLoaderSaver playerLoaderSaver)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(playerLoaderSaver, "playerLoaderSaver");

    this.plugin = plugin;
    this.playerLoaderSaver = playerLoaderSaver;
  }

  @Override
  public void register()
  {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void unregister()
  {
    HandlerList.unregisterAll(this);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoin(PlayerJoinEvent event)
  {
    Player player = event.getPlayer();
    playerLoaderSaver.loadPlayer(player.getName(), player);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuit(PlayerQuitEvent event)
  {
    Player player = event.getPlayer();
    playerLoaderSaver.savePlayer(player.getName(), player);
  }
}

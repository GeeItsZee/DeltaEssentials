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

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.FormatNames;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.Map;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerActionListener implements Listener, Registerable
{
  private final DeltaEssentialsPlugin plugin;
  private final Map<String, DeltaEssPlayerFileWrapper> playerFileWrapperMap;
  private final MessageFormatMap formatMap;

  public PlayerActionListener(
    DeltaEssentialsPlugin plugin, Map<String, DeltaEssPlayerFileWrapper> playerFileWrapperMap,
    MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(playerFileWrapperMap, "playerFileWrapperMap");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.plugin = plugin;
    this.playerFileWrapperMap = playerFileWrapperMap;
    this.formatMap = formatMap;
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

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onInventoryOpen(InventoryOpenEvent event)
  {
    HumanEntity player = event.getPlayer();
    cancelEventIfNotLoaded(player, event, "INVENTORY_OPEN");
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onInventoryInteract(InventoryInteractEvent event)
  {
    HumanEntity player = event.getWhoClicked();
    cancelEventIfNotLoaded(player, event, "INVENTORY_INTERACT");
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerInteractEvent(PlayerInteractEvent event)
  {
    Player player = event.getPlayer();
    cancelEventIfNotLoaded(player, event, "WORLD_INTERACT");
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerDropItemEvent(PlayerDropItemEvent event)
  {
    Player player = event.getPlayer();
    cancelEventIfNotLoaded(player, event, "ITEM_DROP");
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerPickupItemEvent(PlayerPickupItemEvent event)
  {
    Player player = event.getPlayer();
    cancelEventIfNotLoaded(player, event, "ITEM_PICKUP");
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onPlayerDamagedEvent(EntityDamageEvent event)
  {
    if (!(event.getEntity() instanceof Player))
    {
      return;
    }

    Player player = (Player) event.getEntity();
    cancelEventIfNotLoaded(player, event, "TAKING_DAMAGE");
  }

  private void cancelEventIfNotLoaded(
    HumanEntity player, Cancellable cancellable, String actionName)
  {
    DeltaEssPlayerFileWrapper playerFileWrapper = playerFileWrapperMap.get(player.getName());

    if (playerFileWrapper == null)
    {
      player.sendMessage(formatMap.format(FormatNames.ACTION_BLOCKED, actionName, "NOT_LOADED"));
      cancellable.setCancelled(true);
      return;
    }

    if (!playerFileWrapper.wasLoadedByOwner())
    {
      player.sendMessage(formatMap.format(FormatNames.ACTION_BLOCKED, actionName, "LOADED_BY_OTHER"));
      cancellable.setCancelled(true);
      return;
    }

    DeltaEssPlayerFileWrapper.FileState state = playerFileWrapper.getFileState();

    if (state != DeltaEssPlayerFileWrapper.FileState.LOADED)
    {
      player.sendMessage(formatMap.format(FormatNames.ACTION_BLOCKED, actionName, state));
      cancellable.setCancelled(true);
    }
  }
}

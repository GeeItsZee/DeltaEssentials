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

import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.SavedPlayerInventory;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerFileInventoryCloseListener implements Listener, Registerable
{
  private final DeltaEssentialsPlugin plugin;

  public PlayerFileInventoryCloseListener(DeltaEssentialsPlugin plugin)
  {
    Preconditions.checkNotNull(plugin, "plugin");

    this.plugin = plugin;
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

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event)
  {
    Inventory inventory = event.getInventory();
    InventoryHolder holder = inventory.getHolder();

    if (holder == null || !(holder instanceof PlayerFileInventoryHolder))
    {
      return;
    }

    PlayerFileInventoryHolder inventoryHolder = (PlayerFileInventoryHolder) holder;
    String playerFileOwner = inventoryHolder.getPlayerFileOwner();

    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(playerFileOwner);

    // Check if playerFile exists and is loaded
    if (playerFile == null)
    {
      return;
    }

    PlayerFileInventoryHolder.Type type = inventoryHolder.getType();
    SavedPlayerInventory savedInv;

    switch (type)
    {
      case SURVIVAL:
        savedInv = getSavedPlayerInventoryFromInventory(inventory);
        playerFile.setSurvival(savedInv);
        return;
      case CREATIVE:
        savedInv = getSavedPlayerInventoryFromInventory(inventory);
        playerFile.setCreative(savedInv);
        return;
      case ENDER_CHEST:
        playerFile.setEnderChest(inventory.getContents());
        return;
      default:
        throw new IllegalStateException("Unknown inventory type: " + type);
    }
  }

  private SavedPlayerInventory getSavedPlayerInventoryFromInventory(Inventory inventory)
  {
    ItemStack[] storage = new ItemStack[36];
    ItemStack[] armor = new ItemStack[4];
    ItemStack[] extraSlots = new ItemStack[1];

    ItemStack[] itemStacks = inventory.getStorageContents();
    for (int i = 0; i < itemStacks.length; i++)
    {
      if (i < 36)
      {
        storage[i] = itemStacks[i];
      }
      else if (i < 40)
      {
        armor[i - 36] = itemStacks[i];
      }
      else if (i >= 45 && i < 46)
      {
        extraSlots[i - 45] = itemStacks[i];
      }
    }

    return new SavedPlayerInventory(storage, armor, extraSlots);
  }
}

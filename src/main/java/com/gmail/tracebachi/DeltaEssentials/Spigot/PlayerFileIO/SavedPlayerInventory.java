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
package com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO;

import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author GeeItsZee (tracebachi@gmail.com).
 */
public class SavedPlayerInventory
{
  public static final SavedPlayerInventory EMPTY = new SavedPlayerInventory(new ItemStack[36],
    new ItemStack[4], new ItemStack[1]);

  private ItemStack[] storage;
  private ItemStack[] armor;
  private ItemStack[] extraSlots;

  public SavedPlayerInventory(Player player)
  {
    ItemStack[] storage = player.getInventory().getStorageContents();
    ItemStack[] armor = player.getInventory().getArmorContents();
    ItemStack[] extraSlots = player.getInventory().getExtraContents();

    this.storage = (storage != null) ? storage : new ItemStack[36];
    this.armor = (armor != null) ? armor : new ItemStack[4];
    this.extraSlots = (extraSlots != null) ? extraSlots : new ItemStack[1];
  }

  public SavedPlayerInventory(ItemStack[] storage, ItemStack[] armor, ItemStack[] extraSlots)
  {
    Preconditions.checkNotNull(storage, "storage");
    Preconditions.checkNotNull(armor, "armor");
    Preconditions.checkNotNull(extraSlots, "extraSlots");
    Preconditions.checkArgument(storage.length == 36, "storage size must be 36");
    Preconditions.checkArgument(armor.length == 4, "armor size must be 4");
    Preconditions.checkArgument(extraSlots.length == 1, "extraSlots size must be 1");

    this.storage = storage;
    this.armor = armor;
    this.extraSlots = extraSlots;
  }

  public ItemStack[] getStorage()
  {
    return storage;
  }

  public ItemStack[] getArmor()
  {
    return armor;
  }

  public ItemStack[] getExtraSlots()
  {
    return extraSlots;
  }
}

/*
 * This file is part of DeltaEssentials.
 *
 * DeltaEssentials is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaEssentials is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaEssentials.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaEssentials.Storage;

import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class SavedInventory
{
    public static final SavedInventory EMPTY = new SavedInventory(
        new ItemStack[4],
        new ItemStack[36],
        new ItemStack[1]);

    private ItemStack[] armor;
    private ItemStack[] storage;
    private ItemStack[] extraSlots;

    public SavedInventory(Player player)
    {
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] storage = player.getInventory().getStorageContents();
        ItemStack[] extraSlots = player.getInventory().getExtraContents();

        this.armor = (armor != null) ? armor : new ItemStack[4];
        this.storage = (storage != null) ? storage : new ItemStack[36];
        this.extraSlots = (extraSlots != null) ? extraSlots : new ItemStack[1];
    }

    public SavedInventory(ItemStack[] armor, ItemStack[] storage, ItemStack[] extraSlots)
    {
        Preconditions.checkNotNull(armor, "Armor cannot be null.");
        Preconditions.checkNotNull(storage, "Contents cannot be null.");
        Preconditions.checkNotNull(extraSlots, "ExtraSlots cannot be null.");
        Preconditions.checkArgument(armor.length == 4, "Armor size must be 4.");
        Preconditions.checkArgument(storage.length == 36, "Content size must be 36.");
        Preconditions.checkArgument(extraSlots.length == 1, "ExtraSlots size must be 1.");

        this.armor = armor;
        this.storage = storage;
        this.extraSlots = extraSlots;
    }

    public ItemStack[] getArmor()
    {
        return armor;
    }

    public ItemStack[] getStorage()
    {
        return storage;
    }

    public ItemStack[] getExtraSlots()
    {
        return extraSlots;
    }
}

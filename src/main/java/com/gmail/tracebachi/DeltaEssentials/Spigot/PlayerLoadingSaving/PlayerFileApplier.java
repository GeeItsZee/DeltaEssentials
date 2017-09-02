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

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Permissions;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.SavedPlayerInventory;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerFileApplier
{
  private final boolean shouldLoadAndSavePotionEffects;
  private final boolean shouldForceDefaultGameMode;
  private final GameMode defaultGameMode;

  public PlayerFileApplier(
    boolean shouldLoadAndSavePotionEffects, boolean shouldForceDefaultGameMode,
    GameMode defaultGameMode)
  {
    Preconditions.checkArgument(
      !shouldForceDefaultGameMode || (defaultGameMode != null),
      "Non-null GameMode required when forced GameMode is enabled");

    this.shouldLoadAndSavePotionEffects = shouldLoadAndSavePotionEffects;
    this.shouldForceDefaultGameMode = shouldForceDefaultGameMode;
    this.defaultGameMode = defaultGameMode;
  }

  public void applyTo(DeltaEssPlayerFile playerFile, Player player)
  {
    Preconditions.checkNotNull(player, "player");
    Preconditions.checkNotNull(playerFile, "playerFile");

    String gameModePerm = Permissions.GAMEMODE_PERM_PREFIX + defaultGameMode;

    // Enforce the default GameMode if that option is enabled to set the GameMode
    // the player should be in
    if (!shouldForceDefaultGameMode || player.hasPermission(gameModePerm))
    {
      if (player.getGameMode() != playerFile.getGameMode())
      {
        player.setGameMode(playerFile.getGameMode());
      }
    }
    else if (player.getGameMode() != defaultGameMode)
    {
      player.setGameMode(defaultGameMode);
    }

    // Apply the inventory for the player's GameMode
    PlayerInventory playerInventory = player.getInventory();
    if (player.hasPermission(Permissions.GAMEMODE_SHARED_INV) ||
      player.getGameMode() == GameMode.SURVIVAL)
    {
      SavedPlayerInventory survival = playerFile.getSurvival();
      playerInventory.setStorageContents(survival.getStorage());
      playerInventory.setArmorContents(survival.getArmor());
      playerInventory.setExtraContents(survival.getExtraSlots());

      playerFile.setSurvival(null);
    }
    else if (player.getGameMode() == GameMode.CREATIVE)
    {
      SavedPlayerInventory creative = playerFile.getCreative();
      playerInventory.setStorageContents(creative.getStorage());
      playerInventory.setArmorContents(creative.getArmor());
      playerInventory.setExtraContents(creative.getExtraSlots());

      playerFile.setCreative(null);
    }
    else
    {
      playerInventory.clear();
      playerInventory.setArmorContents(new ItemStack[4]);
      playerInventory.setExtraContents(new ItemStack[1]);
    }

    if (shouldLoadAndSavePotionEffects)
    {
      for (PotionEffect effect : player.getActivePotionEffects())
      {
        player.removePotionEffect(effect.getType());
      }

      for (PotionEffect effect : playerFile.getPotionEffects())
      {
        player.addPotionEffect(effect);
      }
    }

    player.setHealth(Math.min(20, playerFile.getHealth()));
    player.setFoodLevel(playerFile.getFoodLevel());
    player.setLevel(playerFile.getXpLevel());
    player.setExp(playerFile.getXpProgress());
    player.getInventory().setHeldItemSlot(playerFile.getHeldItemSlot());
    player.getEnderChest().setContents(playerFile.getEnderChest());
  }

  public void updateFrom(DeltaEssPlayerFile playerFile, Player player)
  {
    Preconditions.checkNotNull(player, "player");
    Preconditions.checkNotNull(playerFile, "playerFile");

    SavedPlayerInventory inventory = new SavedPlayerInventory(player);

    if (player.hasPermission(Permissions.GAMEMODE_SHARED_INV))
    {
      playerFile.setSurvival(inventory);
      playerFile.setCreative(null);
    }
    else if (player.getGameMode() == GameMode.SURVIVAL)
    {
      playerFile.setSurvival(inventory);
    }
    else if (player.getGameMode() == GameMode.CREATIVE)
    {
      playerFile.setCreative(inventory);
    }

    if (shouldLoadAndSavePotionEffects)
    {
      playerFile.setPotionEffects(player.getActivePotionEffects());
    }

    playerFile.setHealth(Math.min(20, player.getHealth()));
    playerFile.setFoodLevel(player.getFoodLevel());
    playerFile.setXpLevel(player.getLevel());
    playerFile.setXpProgress(player.getExp());
    playerFile.setGameMode(player.getGameMode());
    playerFile.setHeldItemSlot(player.getInventory().getHeldItemSlot());
    playerFile.setEnderChest(player.getEnderChest().getContents());
  }
}

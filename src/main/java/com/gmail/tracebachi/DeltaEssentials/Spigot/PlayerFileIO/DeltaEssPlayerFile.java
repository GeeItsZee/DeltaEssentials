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

import com.gmail.tracebachi.DeltaEssentials.Spigot.Chat.SocialSpyLevel;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.Collections;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class DeltaEssPlayerFile
{
  private double health = 20.00D;
  private int foodLevel = 0;
  private int xpLevel = 0;
  private float xpProgress = 0.00F;
  private GameMode gameMode = GameMode.SURVIVAL;
  private int heldItemSlot = 0;
  private Collection<PotionEffect> potionEffects = Collections.emptyList();
  private ItemStack[] enderChest = new ItemStack[27];
  private SavedPlayerInventory survival = SavedPlayerInventory.EMPTY;
  private SavedPlayerInventory creative = SavedPlayerInventory.EMPTY;

  private YamlConfiguration pluginPlayerData = new YamlConfiguration();
  private boolean blockingTeleports = false;
  private boolean vanished = false;
  private String replyingTo = "";
  private SocialSpyLevel socialSpyLevel = SocialSpyLevel.NONE;

  public double getHealth()
  {
    return health;
  }

  public void setHealth(double health)
  {
    this.health = health;
  }

  public int getFoodLevel()
  {
    return foodLevel;
  }

  public void setFoodLevel(int foodLevel)
  {
    this.foodLevel = foodLevel;
  }

  public int getXpLevel()
  {
    return xpLevel;
  }

  public void setXpLevel(int xpLevel)
  {
    this.xpLevel = xpLevel;
  }

  public float getXpProgress()
  {
    return xpProgress;
  }

  public void setXpProgress(float xpProgress)
  {
    this.xpProgress = xpProgress;
  }

  public GameMode getGameMode()
  {
    if (gameMode == null)
    {
      gameMode = GameMode.SURVIVAL;
    }

    return gameMode;
  }

  public void setGameMode(GameMode gameMode)
  {
    this.gameMode = gameMode;
  }

  public int getHeldItemSlot()
  {
    return heldItemSlot;
  }

  public void setHeldItemSlot(int heldItemSlot)
  {
    this.heldItemSlot = heldItemSlot;
  }

  public ItemStack[] getEnderChest()
  {
    if (enderChest == null)
    {
      enderChest = new ItemStack[27];
    }

    return enderChest;
  }

  public void setEnderChest(ItemStack[] enderChest)
  {
    this.enderChest = enderChest;
  }

  public boolean isBlockingTeleports()
  {
    return blockingTeleports;
  }

  public void setBlockingTeleports(boolean blockingTeleports)
  {
    this.blockingTeleports = blockingTeleports;
  }

  public boolean isVanished()
  {
    return vanished;
  }

  public void setVanished(boolean vanished)
  {
    this.vanished = vanished;
  }

  public String getReplyingTo()
  {
    if (replyingTo == null)
    {
      replyingTo = "";
    }

    return replyingTo;
  }

  public void setReplyingTo(String replyingTo)
  {
    this.replyingTo = replyingTo;
  }

  public SocialSpyLevel getSocialSpyLevel()
  {
    if (socialSpyLevel == null)
    {
      socialSpyLevel = SocialSpyLevel.NONE;
    }

    return socialSpyLevel;
  }

  public void setSocialSpyLevel(SocialSpyLevel socialSpyLevel)
  {
    this.socialSpyLevel = socialSpyLevel;
  }

  public YamlConfiguration getPluginPlayerData()
  {
    if (pluginPlayerData == null)
    {
      pluginPlayerData = new YamlConfiguration();
    }

    return pluginPlayerData;
  }

  public void setPluginPlayerData(YamlConfiguration pluginPlayerData)
  {
    this.pluginPlayerData = pluginPlayerData;
  }

  public Collection<PotionEffect> getPotionEffects()
  {
    if (potionEffects == null)
    {
      potionEffects = Collections.emptyList();
    }

    return potionEffects;
  }

  public void setPotionEffects(Collection<PotionEffect> potionEffects)
  {
    this.potionEffects = potionEffects;
  }

  public SavedPlayerInventory getSurvival()
  {
    if (survival == null)
    {
      survival = SavedPlayerInventory.EMPTY;
    }

    return survival;
  }

  public void setSurvival(SavedPlayerInventory survival)
  {
    this.survival = survival;
  }

  public SavedPlayerInventory getCreative()
  {
    if (survival == null)
    {
      survival = SavedPlayerInventory.EMPTY;
    }

    return creative;
  }

  public void setCreative(SavedPlayerInventory creative)
  {
    this.creative = creative;
  }
}

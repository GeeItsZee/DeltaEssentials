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
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/19/16.
 */
public class DeltaEssPlayerData
{
    private static final int ENDER_CHEST_SIZE = 27;

    private final String playerName;
    private double health = 20.00D;
    private int foodLevel = 0;
    private int xpLevel = 0;
    private float xpProgress = 0.00F;
    private GameMode gameMode = GameMode.SURVIVAL;
    private Collection<PotionEffect> potionEffects = Collections.emptyList();
    private ItemStack[] enderChest = new ItemStack[ENDER_CHEST_SIZE];
    private int heldItemSlot = 0;
    private SavedPlayerInventory survival = SavedPlayerInventory.EMPTY;
    private SavedPlayerInventory creative = SavedPlayerInventory.EMPTY;
    private boolean denyingTeleports = false;
    private boolean vanished = false;
    private String replyingTo = "";
    private SocialSpyLevel socialSpyLevel = SocialSpyLevel.NONE;
    private ConfigurationSection metaData = new YamlConfiguration();

    public DeltaEssPlayerData(String playerName)
    {
        Preconditions.checkNotNull(playerName, "playerName");
        Preconditions.checkArgument(!playerName.isEmpty(), "Empty playerName");
        this.playerName = playerName;
    }

    public String getPlayerName()
    {
        return playerName;
    }

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
        return gameMode;
    }

    public void setGameMode(GameMode gameMode)
    {
        Preconditions.checkNotNull(gameMode, "gameMode");
        this.gameMode = gameMode;
    }

    public Collection<PotionEffect> getPotionEffects()
    {
        return potionEffects;
    }

    public void setPotionEffects(Collection<PotionEffect> potionEffects)
    {
        Preconditions.checkNotNull(potionEffects, "potionEffects");
        this.potionEffects = potionEffects;
    }

    public ItemStack[] getEnderChest()
    {
        return enderChest;
    }

    public void setEnderChest(ItemStack[] enderChest)
    {
        Preconditions.checkNotNull(enderChest, "enderChest");
        Preconditions.checkArgument(
            enderChest.length > ENDER_CHEST_SIZE,
            "Too many items in enderChest");
        Preconditions.checkArgument(
            enderChest.length < ENDER_CHEST_SIZE,
            "Too few items in enderChest");

        this.enderChest = enderChest;
    }

    public int getHeldItemSlot()
    {
        return heldItemSlot;
    }

    public void setHeldItemSlot(int heldItemSlot)
    {
        this.heldItemSlot = heldItemSlot;
    }

    public SavedPlayerInventory getSurvival()
    {
        return survival;
    }

    public void setSurvival(SavedPlayerInventory survival)
    {
        this.survival = (survival == null) ? SavedPlayerInventory.EMPTY : survival;
    }

    public void clearSurvival()
    {
        this.survival = SavedPlayerInventory.EMPTY;
    }

    public SavedPlayerInventory getCreative()
    {
        return creative;
    }

    public void setCreative(SavedPlayerInventory creative)
    {
        this.creative = (creative == null) ? SavedPlayerInventory.EMPTY : creative;
    }

    public void clearCreative()
    {
        this.creative = SavedPlayerInventory.EMPTY;
    }

    public boolean isDenyingTeleports()
    {
        return denyingTeleports;
    }

    public void setDenyingTeleports(boolean denyingTeleports)
    {
        this.denyingTeleports = denyingTeleports;
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
        return replyingTo;
    }

    public void setReplyingTo(String replyingTo)
    {
        this.replyingTo = (replyingTo == null) ? "" : replyingTo;
    }

    public SocialSpyLevel getSocialSpyLevel()
    {
        return socialSpyLevel;
    }

    public void setSocialSpyLevel(SocialSpyLevel socialSpyLevel)
    {
        Preconditions.checkNotNull(socialSpyLevel, "socialSpyLevel");
        this.socialSpyLevel = socialSpyLevel;
    }

    public ConfigurationSection getMetaData()
    {
        return (metaData == null) ? new YamlConfiguration() : metaData;
    }

    public void setMetaData(ConfigurationSection metaData)
    {
        Preconditions.checkNotNull(metaData, "metaData");
        this.metaData = metaData;
    }
}

/*
 * This file is part of DeltaInventory.
 *
 * DeltaInventory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaInventory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaInventory.  If not, see <http://www.gnu.org/licenses/>.
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
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerEntry
{
    private final String name;
    private double health;
    private int foodLevel;
    private int xpLevel;
    private double xpProgress;
    private GameMode gameMode;
    private Collection<PotionEffect> potionEffects;
    private boolean socialSpyEnabled;
    private boolean teleportDenyEnabled;
    private String lastReplyTarget;
    private ConfigurationSection metaData;
    private SavedInventory survival;
    private SavedInventory creative;
    private ItemStack[] enderChest;

    public PlayerEntry(String name)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");
        this.name = name.toLowerCase();
    }

    public String getName()
    {
        return name;
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

    public double getXpProgress()
    {
        return xpProgress;
    }

    public void setXpProgress(double xpProgress)
    {
        this.xpProgress = xpProgress;
    }

    public GameMode getGameMode()
    {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode)
    {
        this.gameMode = gameMode;
    }

    public Collection<PotionEffect> getPotionEffects()
    {
        return (potionEffects == null) ? Collections.emptyList() : potionEffects;
    }

    public void setPotionEffects(Collection<PotionEffect> potionEffects)
    {
        this.potionEffects = potionEffects;
    }

    public SavedInventory getSurvival()
    {
        return (survival != null) ? survival : SavedInventory.EMPTY;
    }

    public void setSurvival(SavedInventory survival)
    {
        this.survival = survival;
    }

    public SavedInventory getCreative()
    {
        return (creative != null) ? creative : SavedInventory.EMPTY;
    }

    public void setCreative(SavedInventory creative)
    {
        this.creative = creative;
    }

    public ItemStack[] getEnderChest()
    {
        return (enderChest != null) ? enderChest : new ItemStack[27];
    }

    public void setEnderChest(ItemStack[] enderChest)
    {
        this.enderChest = enderChest;
    }

    public boolean isSocialSpyEnabled()
    {
        return socialSpyEnabled;
    }

    public void setSocialSpyEnabled(boolean socialSpyEnabled)
    {
        this.socialSpyEnabled = socialSpyEnabled;
    }

    public boolean isTeleportDenyEnabled()
    {
        return teleportDenyEnabled;
    }

    public void setTeleportDenyEnabled(boolean teleportDenyEnabled)
    {
        this.teleportDenyEnabled = teleportDenyEnabled;
    }

    public String getLastReplyTarget()
    {
        return (lastReplyTarget != null) ? lastReplyTarget : "";
    }

    public void setLastReplyTarget(String lastReplyTarget)
    {
        this.lastReplyTarget = lastReplyTarget;
    }

    public ConfigurationSection getMetaData()
    {
        return (metaData != null) ? metaData : new YamlConfiguration();
    }

    public void setMetaData(ConfigurationSection metaData)
    {
        this.metaData = metaData;
    }
}

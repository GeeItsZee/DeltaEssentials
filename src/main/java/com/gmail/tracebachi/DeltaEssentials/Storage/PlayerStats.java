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

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 3/21/16.
 */
public class PlayerStats
{
    private double health;
    private int foodLevel;
    private int xpLevel;
    private float xpProgress;
    private GameMode gameMode;
    private ItemStack[] enderChest;

    public PlayerStats(Player player)
    {
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.xpLevel = player.getLevel();
        this.xpProgress = player.getExp();
        this.gameMode = player.getGameMode();
        this.enderChest = player.getEnderChest().getContents();
    }

    public PlayerStats()
    {

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
        this.gameMode = gameMode;
    }

    public ItemStack[] getEnderChest()
    {
        return (enderChest != null) ? enderChest : new ItemStack[27];
    }

    public void setEnderChest(ItemStack[] enderChest)
    {
        this.enderChest = enderChest;
    }
}

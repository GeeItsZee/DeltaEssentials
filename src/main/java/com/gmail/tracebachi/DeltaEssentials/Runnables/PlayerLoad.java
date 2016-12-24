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
package com.gmail.tracebachi.DeltaEssentials.Runnables;

import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedPlayerInventory;
import com.gmail.tracebachi.DeltaEssentials.Storage.SocialSpyLevel;
import com.gmail.tracebachi.DeltaEssentials.Utils.InventoryUtils;
import com.gmail.tracebachi.DeltaEssentials.Utils.LockedFileUtil;
import com.gmail.tracebachi.DeltaEssentials.Utils.PotionEffectUtils;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerLoad implements Runnable
{
    /**
     * Interface for handing different outcomes of running this Runnable
     */
    public interface Callbacks
    {
        void onSuccess(DeltaEssPlayerData playerData);

        void onNotFoundFailure();

        void onExceptionFailure(Exception ex);
    }

    private final String playerName;
    private final File fileToLoad;
    private final Callbacks callbacks;

    public PlayerLoad(String playerName, File fileToLoad, Callbacks callbacks)
    {
        Preconditions.checkNotNull(playerName, "playerName");
        Preconditions.checkNotNull(fileToLoad, "fileToLoad");
        Preconditions.checkNotNull(callbacks, "callbacks");
        Preconditions.checkArgument(!playerName.isEmpty(), "Empty playerName");

        this.playerName = playerName;
        this.fileToLoad = fileToLoad;
        this.callbacks = callbacks;
    }

    @Override
    public void run()
    {
        if(!fileToLoad.exists())
        {
            callbacks.onNotFoundFailure();
            return;
        }

        try
        {
            String fileContents = LockedFileUtil.read(fileToLoad);
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(fileContents);

            DeltaEssPlayerData playerData = readDeltaEssPlayerData(configuration);
            callbacks.onSuccess(playerData);
        }
        catch(IOException | InvalidConfigurationException e)
        {
            callbacks.onExceptionFailure(e);
        }
    }

    private DeltaEssPlayerData readDeltaEssPlayerData(YamlConfiguration config)
    {
        DeltaEssPlayerData playerData = new DeltaEssPlayerData(playerName);
        ItemStack[] itemStacks;
        ConfigurationSection section;
        SavedPlayerInventory savedPlayerInventory;

        playerData.setHealth(
            config.getDouble("Health", 20.0));
        playerData.setFoodLevel(
            config.getInt("FoodLevel", 20));
        playerData.setXpLevel(
            config.getInt("XpLevel", 0));
        playerData.setXpProgress(
            (float) config.getDouble("XpProgress", 0.0));
        playerData.setGameMode(parseGameMode(
            config.getString("Gamemode", "SURVIVAL")));
        playerData.setPotionEffects(PotionEffectUtils.toEffectList(
            config.getStringList("Effects")));
        playerData.setHeldItemSlot(
            config.getInt("HeldItemSlot", 0));
        playerData.setSocialSpyLevel(parseSocialSpyLevel(
            config.getString("SocialSpyLevel", "NONE")));
        playerData.setDenyingTeleports(
            config.getBoolean("DenyingTeleports", false));
        playerData.setVanished(
            config.getBoolean("Vanished", false));
        playerData.setReplyingTo(
            config.getString("ReplyingTo", ""));
        playerData.setMetaData(
            config.getConfigurationSection("MetaData"));

        section = config.getConfigurationSection("Survival");
        savedPlayerInventory = InventoryUtils.toSavedInventory(section);
        playerData.setSurvival(savedPlayerInventory);

        section = config.getConfigurationSection("Creative");
        savedPlayerInventory = InventoryUtils.toSavedInventory(section);
        playerData.setCreative(savedPlayerInventory);

        section = config.getConfigurationSection("EnderChest");
        itemStacks = InventoryUtils.toItemStacks(section, 27);
        playerData.setEnderChest(itemStacks);

        return playerData;
    }

    private GameMode parseGameMode(String input)
    {
        try
        {
            return (input != null) ? GameMode.valueOf(input) : GameMode.SURVIVAL;
        }
        catch(IllegalArgumentException e)
        {
            return GameMode.SURVIVAL;
        }
    }

    private SocialSpyLevel parseSocialSpyLevel(String input)
    {
        try
        {
            return (input != null) ? SocialSpyLevel.valueOf(input) : SocialSpyLevel.NONE;
        }
        catch(IllegalArgumentException e)
        {
            return SocialSpyLevel.NONE;
        }
    }
}

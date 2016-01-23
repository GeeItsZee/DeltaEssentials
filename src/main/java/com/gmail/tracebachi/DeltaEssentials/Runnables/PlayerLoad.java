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

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.Listeners.PlayerDataIOListener;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerEntry;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedInventory;
import com.gmail.tracebachi.DeltaEssentials.Utils.InventoryUtils;
import com.gmail.tracebachi.DeltaEssentials.Utils.LockedFileUtil;
import com.gmail.tracebachi.DeltaEssentials.Utils.PotionEffectUtils;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerLoad implements Runnable
{
    private final String name;
    private final File playerDataFile;
    private final DeltaEssentials plugin;

    public PlayerLoad(String name, DeltaEssentials plugin)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");
        Preconditions.checkNotNull(plugin, "Plugin cannot be null.");

        this.name = name.toLowerCase();
        this.playerDataFile = plugin.getSettings().getPlayerDataFileFor(this.name);
        this.plugin = plugin;
    }

    @Override
    public void run()
    {
        if(!playerDataFile.exists())
        {
            onNotFoundFailure();
            return;
        }

        try
        {
            String fileContents = LockedFileUtil.read(playerDataFile);
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(fileContents);

            PlayerEntry entry = readPlayerDataYaml(configuration);
            onSuccess(entry);
        }
        catch(IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            onExceptionFailure();
        }
    }

    private void onSuccess(PlayerEntry entry)
    {
        plugin.debug("Loaded player data for {name:" + entry.getName() + "}");
        plugin.scheduleTaskSync(() ->
        {
            PlayerDataIOListener listener = plugin.getPlayerDataIOListener();
            listener.onPlayerLoadSuccess(name, entry);
        });
    }

    private void onNotFoundFailure()
    {
        plugin.debug("Player data not found for {name:" + name + "}");
        plugin.scheduleTaskSync(() ->
        {
            PlayerDataIOListener listener = plugin.getPlayerDataIOListener();
            listener.onPlayerNotFound(name);
        });
    }

    private void onExceptionFailure()
    {
        plugin.debug("Failed to load player data for {name:" + name + "} due to an exception");
        plugin.scheduleTaskSync(() ->
        {
            PlayerDataIOListener listener = plugin.getPlayerDataIOListener();
            listener.onPlayerLoadException(name);
        });
    }

    private PlayerEntry readPlayerDataYaml(YamlConfiguration config)
    {
        ItemStack[] itemStacks;
        ConfigurationSection section;
        SavedInventory savedInventory;
        List<PotionEffect> effects;
        PlayerEntry entry = new PlayerEntry(name);

        entry.setHealth(config.getDouble("Health", 20.0));
        entry.setFoodLevel(config.getInt("Hunger", 20));
        entry.setXpLevel(config.getInt("XpLevel", 0));
        entry.setXpProgress(config.getDouble("XpProgress", 0.0));
        entry.setGameMode(GameMode.valueOf(config.getString("Gamemode", "SURVIVAL")));
        entry.setSocialSpyEnabled(config.getBoolean("SocialSpyEnabled", false));
        entry.setTeleportDenyEnabled(config.getBoolean("TeleportDenyEnabled", false));
        entry.setLastReplyTarget(config.getString("LastReplyTarget", ""));
        entry.setMetaData(config.getConfigurationSection("MetaData"));

        effects = PotionEffectUtils.toEffectList(config.getStringList("Effects"));
        entry.setPotionEffects(effects);

        section = config.getConfigurationSection("Survival");
        savedInventory = InventoryUtils.toSavedInventory(section);
        entry.setSurvival(savedInventory);

        section = config.getConfigurationSection("Creative");
        savedInventory = InventoryUtils.toSavedInventory(section);
        entry.setCreative(savedInventory);

        section = config.getConfigurationSection("EnderChest");
        itemStacks = InventoryUtils.toItemStacks(section, 27);
        entry.setEnderChest(itemStacks);
        return entry;
    }
}

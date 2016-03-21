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
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerEntry;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerStats;
import com.gmail.tracebachi.DeltaEssentials.Utils.InventoryUtils;
import com.gmail.tracebachi.DeltaEssentials.Utils.LockedFileUtil;
import com.gmail.tracebachi.DeltaEssentials.Utils.PotionEffectUtils;
import com.google.common.base.Preconditions;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerSave implements Runnable
{
    private final String destServer;
    private final PlayerEntry entry;
    private final PlayerDataIOListener listener;
    private final DeltaEssentials plugin;

    public PlayerSave(PlayerEntry entry, String destServer, PlayerDataIOListener listener, DeltaEssentials plugin)
    {
        Preconditions.checkNotNull(entry, "Entry cannot be null.");
        Preconditions.checkNotNull(plugin, "Plugin cannot be null.");

        this.entry = entry;
        this.destServer = destServer;
        this.listener = listener;
        this.plugin = plugin;
    }

    @Override
    public void run()
    {
        File playerDataFile = Settings.getPlayerDataFileFor(entry.getName());

        try
        {
            YamlConfiguration configuration = writePlayerDataYaml();
            String source = configuration.saveToString();

            if(LockedFileUtil.write(source, playerDataFile))
            {
                onSuccess();
            }
            else
            {
                onFailure();
            }
        }
        catch(IOException | NullPointerException e)
        {
            e.printStackTrace();
            onFailure();
        }
    }

    private void onSuccess()
    {
        plugin.debug("Saved inventory for {name:" + entry.getName() + "}");
        plugin.scheduleTaskSync(
            () -> listener.onPlayerSaveSuccess(entry.getName(), destServer));
    }

    private void onFailure()
    {
        plugin.debug("Failed to save inventory for {name:" + entry.getName() + "} due to an exception");
        plugin.scheduleTaskSync(
            () -> listener.onPlayerSaveException(entry.getName()));
    }

    private YamlConfiguration writePlayerDataYaml()
    {
        YamlConfiguration serialized;
        YamlConfiguration config = new YamlConfiguration();
        PlayerStats playerStats = entry.getPlayerStats();
        DeltaEssPlayerData playerData = entry.getDeltaEssPlayerData();

        config.set("LastSave", System.currentTimeMillis());
        config.set("Health", playerStats.getHealth());
        config.set("Hunger", playerStats.getFoodLevel());
        config.set("XpLevel", playerStats.getXpLevel());
        config.set("XpProgress", playerStats.getXpProgress());
        config.set("Gamemode", playerStats.getGameMode().toString());
        config.set("Effects", PotionEffectUtils.toStringList(playerStats.getPotionEffects()));
        config.set("SocialSpyEnabled", playerData.isSocialSpyEnabled());
        config.set("TeleportDenyEnabled", playerData.isTeleportDenyEnabled());
        config.set("ReplyTo", playerData.getReplyTo());
        config.set("MetaData", entry.getMetaData());

        serialized = InventoryUtils.toYamlSection(playerData.getSurvival().getArmor());
        config.set("Survival.Armor", serialized);
        serialized = InventoryUtils.toYamlSection(playerData.getSurvival().getContents());
        config.set("Survival.Contents", serialized);

        serialized = InventoryUtils.toYamlSection(playerData.getCreative().getArmor());
        config.set("Creative.Armor", serialized);
        serialized = InventoryUtils.toYamlSection(playerData.getCreative().getContents());
        config.set("Creative.Contents", serialized);

        serialized = InventoryUtils.toYamlSection(playerStats.getEnderChest());
        config.set("EnderChest", serialized);
        return config;
    }
}

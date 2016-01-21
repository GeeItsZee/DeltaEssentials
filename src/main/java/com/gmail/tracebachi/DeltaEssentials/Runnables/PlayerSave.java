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
package com.gmail.tracebachi.DeltaEssentials.Runnables;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.Listeners.PlayerDataIOListener;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerEntry;
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
    private final File playerDataFile;
    private final DeltaEssentials plugin;

    public PlayerSave(PlayerEntry entry, DeltaEssentials plugin)
    {
        this(entry, null, plugin);
    }

    public PlayerSave(PlayerEntry entry, String destServer, DeltaEssentials plugin)
    {
        Preconditions.checkNotNull(entry, "Entry cannot be null.");
        Preconditions.checkNotNull(plugin, "Plugin cannot be null.");

        this.entry = entry;
        this.playerDataFile = plugin.getSettings().getPlayerDataFileFor(entry.getName());
        this.destServer = destServer;
        this.plugin = plugin;
    }

    @Override
    public void run()
    {
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
        plugin.scheduleTaskSync(() ->
        {
            PlayerDataIOListener listener = plugin.getPlayerDataIOListener();
            listener.onPlayerSaveSuccess(entry.getName(), destServer);
        });
    }

    private void onFailure()
    {
        plugin.debug("Failed to save inventory for {name:" + entry.getName() + "} due to an exception");
        plugin.scheduleTaskSync(() ->
        {
            PlayerDataIOListener listener = plugin.getPlayerDataIOListener();
            listener.onPlayerSaveException(entry.getName());
        });
    }

    private YamlConfiguration writePlayerDataYaml()
    {
        YamlConfiguration serialized;
        YamlConfiguration config = new YamlConfiguration();

        config.set("LastSave", System.currentTimeMillis());
        config.set("Health", entry.getHealth());
        config.set("Hunger", entry.getFoodLevel());
        config.set("XpLevel", entry.getXpLevel());
        config.set("XpProgress", entry.getXpProgress());
        config.set("Gamemode", entry.getGameMode().toString());
        config.set("Effects", PotionEffectUtils.toStringList(entry.getPotionEffects()));
        config.set("SocialSpyEnabled", entry.isSocialSpyEnabled());
        config.set("TeleportDenyEnabled", entry.isTeleportDenyEnabled());
        config.set("LastReplyTarget", entry.getLastReplyTarget());
        config.set("MetaData", entry.getMetaData());

        serialized = InventoryUtils.toYamlSection(entry.getSurvival().getArmor());
        config.set("Survival.Armor", serialized);
        serialized = InventoryUtils.toYamlSection(entry.getSurvival().getContents());
        config.set("Survival.Contents", serialized);

        serialized = InventoryUtils.toYamlSection(entry.getCreative().getArmor());
        config.set("Creative.Armor", serialized);
        serialized = InventoryUtils.toYamlSection(entry.getCreative().getContents());
        config.set("Creative.Contents", serialized);

        serialized = InventoryUtils.toYamlSection(entry.getEnderChest());
        config.set("EnderChest", serialized);
        return config;
    }
}

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
    /**
     * Interface for handing different outcomes of running this Runnable
     */
    public interface Callbacks
    {
        void onSuccess();

        void onFailure(Exception ex);
    }

    private final DeltaEssPlayerData playerData;
    private final File fileToSave;
    private final Callbacks callbacks;

    public PlayerSave(DeltaEssPlayerData playerData, File fileToSave, Callbacks callbacks)
    {
        Preconditions.checkNotNull(playerData, "playerData");
        Preconditions.checkNotNull(fileToSave, "fileToSave");
        Preconditions.checkNotNull(callbacks, "callbacks");

        this.playerData = playerData;
        this.fileToSave = fileToSave;
        this.callbacks = callbacks;
    }

    @Override
    public void run()
    {
        try
        {
            YamlConfiguration configuration = writePlayerDataYaml();
            String source = configuration.saveToString();

            if(LockedFileUtil.write(source, fileToSave))
            {
                callbacks.onSuccess();
            }
            else
            {
                callbacks.onFailure(null);
            }
        }
        catch(IOException | NullPointerException ex)
        {
            callbacks.onFailure(ex);
        }
    }

    private YamlConfiguration writePlayerDataYaml()
    {
        YamlConfiguration config = new YamlConfiguration();

        config.set("LastSave", System.currentTimeMillis());
        config.set("Health", playerData.getHealth());
        config.set("FoodLevel", playerData.getFoodLevel());
        config.set("XpLevel", playerData.getXpLevel());
        config.set("XpProgress", playerData.getXpProgress());
        config.set("Gamemode", playerData.getGameMode().toString());
        config.set("Effects", PotionEffectUtils.toStringList(playerData.getPotionEffects()));
        config.set("HeldItemSlot", playerData.getHeldItemSlot());

        YamlConfiguration serialized;

        serialized = InventoryUtils.toYamlSection(playerData.getSurvival().getArmor());
        config.set("Survival.Armor", serialized);
        serialized = InventoryUtils.toYamlSection(playerData.getSurvival().getStorage());
        config.set("Survival.Contents", serialized);
        serialized = InventoryUtils.toYamlSection(playerData.getSurvival().getExtraSlots());
        config.set("Survival.ExtraSlots", serialized);

        serialized = InventoryUtils.toYamlSection(playerData.getCreative().getArmor());
        config.set("Creative.Armor", serialized);
        serialized = InventoryUtils.toYamlSection(playerData.getCreative().getStorage());
        config.set("Creative.Contents", serialized);
        serialized = InventoryUtils.toYamlSection(playerData.getCreative().getExtraSlots());
        config.set("Creative.ExtraSlots", serialized);

        serialized = InventoryUtils.toYamlSection(playerData.getEnderChest());
        config.set("EnderChest", serialized);

        config.set("DenyingTeleports", playerData.isDenyingTeleports());
        config.set("Vanished", playerData.isVanished());
        config.set("ReplyingTo", playerData.getReplyingTo());
        config.set("SocialSpyLevel", playerData.getSocialSpyLevel().toString());
        config.set("MetaData", playerData.getMetaData());
        return config;
    }
}

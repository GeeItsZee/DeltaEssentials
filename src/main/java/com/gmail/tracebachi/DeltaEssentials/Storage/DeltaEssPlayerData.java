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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/19/16.
 */
public class DeltaEssPlayerData
{
    private boolean teleportDenyEnabled;
    private boolean vanishEnabled;
    private String socialSpyLevel;
    private String replyTo;
    private Collection<PotionEffect> potionEffects;
    private ConfigurationSection metaData;
    private SavedInventory survival;
    private SavedInventory creative;

    public String getSocialSpyLevel()
    {
        return (socialSpyLevel == null) ? "OFF" : socialSpyLevel;
    }

    public void setSocialSpyLevel(String socialSpyLevel)
    {
        String uppercased = socialSpyLevel.toUpperCase();

        if(!uppercased.equals("ALL") && !uppercased.equals("WORLD") && !uppercased.equals("OFF"))
        {
            throw new IllegalArgumentException("SocialSpyLevel can only be ALL, WORLD, or OFF.");
        }

        this.socialSpyLevel = uppercased;
    }

    public boolean isTeleportDenyEnabled()
    {
        return teleportDenyEnabled;
    }

    public void setTeleportDenyEnabled(boolean teleportDenyEnabled)
    {
        this.teleportDenyEnabled = teleportDenyEnabled;
    }

    public boolean isVanishEnabled()
    {
        return vanishEnabled;
    }

    public void setVanishEnabled(boolean vanishEnabled)
    {
        this.vanishEnabled = vanishEnabled;
    }

    public String getReplyTo()
    {
        return (replyTo != null) ? replyTo : "";
    }

    public void setReplyTo(String replyTo)
    {
        this.replyTo = replyTo;
    }

    public Collection<PotionEffect> getPotionEffects()
    {
        return (potionEffects == null) ? Collections.emptyList() : potionEffects;
    }

    public void setPotionEffects(Collection<PotionEffect> potionEffects)
    {
        this.potionEffects = potionEffects;
    }

    public ConfigurationSection getMetaData()
    {
        return (metaData == null) ? new YamlConfiguration() : metaData;
    }

    public void setMetaData(ConfigurationSection metaData)
    {
        this.metaData = metaData;
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
}

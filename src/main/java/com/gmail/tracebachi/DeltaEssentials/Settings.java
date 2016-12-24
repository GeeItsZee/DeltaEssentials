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
package com.gmail.tracebachi.DeltaEssentials;

import com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashSet;
import com.google.common.base.Preconditions;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/20/16.
 */
public class Settings
{
    private static final String PLAYERDATA_FOLDER_PREFIX =
        "plugins" + File.separator +
        "DeltaEssentials" + File.separator +
        "PlayerData" + File.separator;

    private boolean debugEnabled;
    private boolean loadPlayerDataOnLogin;
    private CaseInsensitiveHashSet privateServers;
    private GameMode defaultGameMode;
    private boolean forceDefaultGameModeOnJoin;
    private Set<GameMode> disabledGameModes;
    private boolean loadAndSavePotionEffects;
    private List<String> preSaveCommands;

    public void read(FileConfiguration config)
    {
        disabledGameModes = new HashSet<>();
        privateServers = new CaseInsensitiveHashSet();
        preSaveCommands = new ArrayList<>();

        debugEnabled = config.getBoolean("Debug", false);
        loadPlayerDataOnLogin = config.getBoolean("LoadPlayerDataOnLogin", true);
        privateServers.addAll(config.getStringList("PrivateServers"));
        defaultGameMode = getGameMode(config.getString("DefaultGameMode", "SURVIVAL"));
        forceDefaultGameModeOnJoin = config.getBoolean("ForceDefaultGameModeOnJoin", false);
        loadAndSavePotionEffects = config.getBoolean("LoadAndSavePotionEffects", false);
        preSaveCommands.addAll(config.getStringList("PreSaveCommands"));
        preSaveCommands = Collections.unmodifiableList(preSaveCommands);

        for(String modeName : config.getStringList("DisabledGameModes"))
        {
            disabledGameModes.add(getGameMode(modeName));
        }

        ConfigurationSection section = config.getConfigurationSection("Formats");
        if(section != null)
        {
            for(String formatKey : section.getKeys(false))
            {
                String rawFormat = section.getString(formatKey);
                String format = ChatColor.translateAlternateColorCodes('&', rawFormat);
                ChatMessageHelper.instance().updateFormat("DeltaEss." + formatKey, format);
            }
        }
    }

    public boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled)
    {
        this.debugEnabled = debugEnabled;
    }

    public boolean shouldLoadPlayerDataOnLogin()
    {
        return loadPlayerDataOnLogin;
    }

    public CaseInsensitiveHashSet getPrivateServers()
    {
        return privateServers;
    }

    public GameMode getDefaultGameMode()
    {
        return defaultGameMode;
    }

    public boolean shouldForceDefaultGameModeOnJoin()
    {
        return forceDefaultGameModeOnJoin;
    }

    public Set<GameMode> getDisabledGameModes()
    {
        return disabledGameModes;
    }

    public boolean shouldLoadAndSavePotionEffects()
    {
        return loadAndSavePotionEffects;
    }

    public List<String> getPreSaveCommands()
    {
        return preSaveCommands;
    }

    public File getPlayerDataFileFor(String name)
    {
        Preconditions.checkNotNull(name, "playerName");
        name = name.toLowerCase();

        File directory = new File(PLAYERDATA_FOLDER_PREFIX + name.charAt(0));
        directory.mkdirs();

        return new File(directory, name + ".yml");
    }

    private static GameMode getGameMode(String source)
    {
        try
        {
            return GameMode.valueOf(source);
        }
        catch(NullPointerException | IllegalArgumentException ex)
        {
            return GameMode.SURVIVAL;
        }
    }
}

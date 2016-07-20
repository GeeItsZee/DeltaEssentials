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

import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashSet;
import com.google.common.base.Preconditions;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/20/16.
 */
public class Settings
{
    private static volatile boolean debugEnabled;
    private static volatile boolean syncTaskSchedulingAllowed;
    private static boolean inLockdown;
    private static boolean defaultGameModeForced;
    private static boolean ignorePotionEffects;
    private static boolean loadPlayerDataOnLogin;
    private static GameMode defaultGameMode;
    private static Set<GameMode> disabledGameModes;
    private static CaseInsensitiveHashSet blockedServers;
    private static List<String> preSaveCommands;
    private static HashMap<String, MessageFormat> formatMap;

    private Settings() {}

    public static void read(FileConfiguration config)
    {
        disabledGameModes = new HashSet<>();
        blockedServers = new CaseInsensitiveHashSet();
        preSaveCommands = new ArrayList<>();
        formatMap = new HashMap<>();

        debugEnabled = config.getBoolean("Debug");
        inLockdown = config.getBoolean("StartWithLockdown", false);
        loadPlayerDataOnLogin = config.getBoolean("LoadPlayerDataOnLogin", true);
        defaultGameMode = getGameMode(config.getString("GameModes.Default", "SURVIVAL"));
        defaultGameModeForced = config.getBoolean("GameModes.ForceDefault", false);
        ignorePotionEffects = config.getBoolean("IgnorePotionEffects", false);
        blockedServers.addAll(config.getStringList("BlockedServers"));
        preSaveCommands.addAll(config.getStringList("PreSaveCommands"));

        for(String modeName : config.getStringList("GameModes.DisabledModes"))
        {
            disabledGameModes.add(getGameMode(modeName));
        }

        ConfigurationSection section = config.getConfigurationSection("Formats");

        if(section != null)
        {
            for(String formatKey : section.getKeys(false))
            {
                String format = ChatColor.translateAlternateColorCodes(
                    '&', section.getString(formatKey));

                formatMap.put(formatKey, new MessageFormat(format));
            }
        }
    }

    public static File getPlayerDataFileFor(String playerName)
    {
        String lowerCaseName = playerName.toLowerCase();
        File directory = new File(
            "plugins" + File.separator +
            "DeltaEssentials" + File.separator +
            "PlayerData" + File.separator +
            lowerCaseName.charAt(0));

        directory.mkdirs();

        return new File(directory, lowerCaseName + ".yml");
    }

    public static boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean debugEnabled)
    {
        Settings.debugEnabled = debugEnabled;
    }

    public static boolean isSyncTaskSchedulingAllowed()
    {
        return syncTaskSchedulingAllowed;
    }

    public static void setSyncTaskSchedulingAllowed(boolean syncTaskSchedulingAllowed)
    {
        Settings.syncTaskSchedulingAllowed = syncTaskSchedulingAllowed;
    }

    public static boolean isInLockdown()
    {
        return inLockdown;
    }

    public static void setInLockdown(boolean inLockdown)
    {
        Settings.inLockdown = inLockdown;
    }

    public static boolean shouldIgnorePotionEffects()
    {
        return ignorePotionEffects;
    }

    public static boolean shouldLoadPlayerDataOnLogin()
    {
        return loadPlayerDataOnLogin;
    }

    public static GameMode getDefaultGameMode()
    {
        return defaultGameMode;
    }

    public static boolean isDefaultGameModeForced()
    {
        return defaultGameModeForced;
    }

    public static boolean isGameModeDisabled(GameMode gameMode)
    {
        return disabledGameModes.contains(gameMode);
    }

    public static boolean isServerBlocked(String serverName)
    {
        return blockedServers.contains(serverName);
    }

    public static boolean isGameModeBlocked(GameMode mode)
    {
        if(disabledGameModes.contains(mode)) return true;

        if(defaultGameModeForced && mode != defaultGameMode) return true;

        return false;
    }

    public static void runPreSaveCommands(Player player)
    {
        Preconditions.checkNotNull(player);

        preSaveCommands.forEach(player::performCommand);
    }

    public static String format(String key, String... args)
    {
        MessageFormat messageFormat = formatMap.get(key);

        if(messageFormat != null)
        {
            return messageFormat.format(args);
        }
        else
        {
            return "Unspecified format: " + key;
        }
    }

    private static GameMode getGameMode(String source)
    {
        if(source == null) { return GameMode.SURVIVAL; }

        switch(source.toUpperCase())
        {
            default:
            case "SURVIVAL":
                return GameMode.SURVIVAL;
            case "CREATIVE":
                return GameMode.CREATIVE;
            case "ADVENTURE":
                return GameMode.ADVENTURE;
            case "SPECTATOR":
                return GameMode.SPECTATOR;
        }
    }
}

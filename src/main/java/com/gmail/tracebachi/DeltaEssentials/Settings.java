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
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/20/16.
 */
public class Settings
{
    private boolean startWithLockdown;
    private boolean isDefaultGameModeForced;
    private boolean loadAndSavePotionEffects;
    private String lockdownMessage;
    private String playerDataFolderPath;
    private String jailServer;
    private GameMode defaultGameMode;
    private Set<GameMode> disabledGameModes;
    private Map<String, String> sharedChatChannels;
    private List<String> validJails;
    private CaseInsensitiveHashSet blockedServers;
    private HashMap<String, MessageFormat> formatMap;

    public Settings(DeltaEssentials plugin)
    {
        this.startWithLockdown = plugin.getConfig().getBoolean("StartWithLockdown");
        this.isDefaultGameModeForced = plugin.getConfig().getBoolean("ForceDefaultGameMode");
        this.loadAndSavePotionEffects = plugin.getConfig().getBoolean("LoadAndSavePotionEffects");
        this.lockdownMessage = plugin.getConfig().getString("LockdownMessage");
        this.playerDataFolderPath = plugin.getConfig().getString("PlayerDataFolder");
        this.jailServer = plugin.getConfig().getString("JailServer");
        this.defaultGameMode = getGameMode(plugin.getConfig().getString("DefaultGameMode"));
        this.validJails = plugin.getConfig().getStringList("ValidJails");
        this.disabledGameModes = new HashSet<>();
        this.sharedChatChannels = new HashMap<>();
        this.blockedServers = new CaseInsensitiveHashSet();
        this.formatMap = new HashMap<>();

        ConfigurationSection section;

        section = plugin.getConfig().getConfigurationSection("SharedChatChannels");
        if(section != null)
        {
            for(String channel : section.getKeys(false))
            {
                this.sharedChatChannels.put(channel, section.getString(channel));
            }
        }

        section = plugin.getConfig().getConfigurationSection("Formats");
        if(section != null)
        {
            for(String formatKey : section.getKeys(false))
            {
                String format = section.getString(formatKey);
                this.formatMap.put(formatKey, new MessageFormat(format));
            }
        }

        for(String modeName : plugin.getConfig().getStringList("DisabledGameModes"))
        {
            try
            {
                this.disabledGameModes.add(GameMode.valueOf(modeName.toUpperCase()));
            }
            catch(IllegalArgumentException ignore) {}
        }

        for(String server : plugin.getConfig().getStringList("BlockedServers"))
        {
            this.blockedServers.add(server);
        }

        File file = new File(playerDataFolderPath);
        file.mkdirs();
    }

    public File getPlayerDataFileFor(String name)
    {
        String lowerCaseName = name.toLowerCase();
        File directory = new File(playerDataFolderPath +
            File.separator + lowerCaseName.charAt(0));

        directory.mkdirs();
        return new File(directory, lowerCaseName + ".yml");
    }

    public boolean isStartWithLockdown()
    {
        return startWithLockdown;
    }

    public boolean canLoadAndSavePotionEffects()
    {
        return loadAndSavePotionEffects;
    }

    public String getLockdownMessage()
    {
        return lockdownMessage;
    }

    public String getJailServer()
    {
        return jailServer;
    }

    public GameMode getDefaultGameMode()
    {
        return defaultGameMode;
    }

    public boolean isDefaultGameModeForced()
    {
        return isDefaultGameModeForced;
    }

    public boolean isGameModeDisabled(GameMode gameMode)
    {
        return disabledGameModes.contains(gameMode);
    }

    public String getSharedChatChannelPermission(String channel)
    {
        return sharedChatChannels.get(channel);
    }

    public boolean isServerBlocked(String serverName)
    {
        return blockedServers.contains(serverName);
    }

    public boolean isValidJail(String jail)
    {
        return validJails.contains(jail);
    }

    public String format(String key, String... args)
    {
        MessageFormat messageFormat = formatMap.get(key);
        if(messageFormat != null)
        {
            return messageFormat.format(args);
        }
        else
        {
            return "Format (" + key + ") has not been specified.";
        }
    }

    private GameMode getGameMode(String source)
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

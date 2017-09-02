/*
 * DeltaEssentials - Player data, chat, and teleport plugin for BungeeCord and Spigot servers
 * Copyright (C) 2017 tracebachi@gmail.com (GeeItsZee)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaEssentials.Spigot;

import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
class DeltaEssentialsConfiguration
{
  private GameMode defaultGameMode;
  private boolean forceDefaultGameMode;
  private ItemStack[] firstJoinItemStacks;
  private Set<GameMode> blockedGameModes;
  private boolean loadAndSavePotionEffects;
  private boolean debugMode;
  private List<String> preSaveCommands;
  private MessageFormatMap formatMap;

  void read(FileConfiguration config)
  {
    firstJoinItemStacks = new ItemStack[0];
    blockedGameModes = new HashSet<>();
    preSaveCommands = new ArrayList<>();

    defaultGameMode = getGameMode(config.getString("DefaultGameMode", "SURVIVAL"));
    forceDefaultGameMode = config.getBoolean("ForceDefaultGameMode", false);

    try
    {
      List<ItemStack> firstJoinItemStacksList = (List<ItemStack>) config.get("FirstJoinItemStacks");
      int listSize = firstJoinItemStacksList.size();

      this.firstJoinItemStacks = firstJoinItemStacksList.toArray(new ItemStack[listSize]);
    }
    catch (ClassCastException ex)
    {
      ex.printStackTrace();
      firstJoinItemStacks = new ItemStack[0];
    }

    for (String modeName : config.getStringList("BlockedGameModes"))
    {
      GameMode gameMode = getGameMode(modeName);
      if (gameMode != null)
      {
        blockedGameModes.add(gameMode);
      }
    }

    loadAndSavePotionEffects = config.getBoolean("LoadAndSavePotionEffects", false);

    preSaveCommands.addAll(config.getStringList("PreSaveCommands"));

    formatMap = new MessageFormatMap();

    ConfigurationSection section = config.getConfigurationSection("Formats");
    if (section != null)
    {
      for (String formatKey : section.getKeys(false))
      {
        String rawFormat = section.getString(formatKey);
        String format = ChatColor.translateAlternateColorCodes('&', rawFormat);
        formatMap.put(formatKey, format);
      }
    }

    debugMode = config.getBoolean("DebugMode", false);
  }

  boolean inDebugMode()
  {
    return debugMode;
  }

  GameMode getDefaultGameMode()
  {
    return defaultGameMode;
  }

  boolean shouldForceDefaultGameMode()
  {
    return forceDefaultGameMode;
  }

  ItemStack[] getFirstJoinItemStacks()
  {
    return firstJoinItemStacks;
  }

  Set<GameMode> getBlockedGameModes()
  {
    return blockedGameModes;
  }

  boolean shouldLoadAndSavePotionEffects()
  {
    return loadAndSavePotionEffects;
  }

  List<String> getPreSaveCommands()
  {
    return preSaveCommands;
  }

  MessageFormatMap getFormatMap()
  {
    return formatMap;
  }

  private static GameMode getGameMode(String source)
  {
    try
    {
      return GameMode.valueOf(source);
    }
    catch (NullPointerException | IllegalArgumentException ex)
    {
      return null;
    }
  }
}

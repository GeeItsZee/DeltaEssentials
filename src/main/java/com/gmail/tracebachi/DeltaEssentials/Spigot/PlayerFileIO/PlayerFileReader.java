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
package com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.PlayerDataFileKeys;
import com.gmail.tracebachi.DeltaEssentials.Spigot.Chat.SocialSpyLevel;
import com.gmail.tracebachi.SockExchange.Utilities.ExtraPreconditions;
import com.gmail.tracebachi.SockExchange.Utilities.Pair;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerFileReader
{
  private final Path minecraftPlayerDataFolderPath;
  private final Path pluginPlayerDataFolderPath;

  public PlayerFileReader(
    Path minecraftPlayerDataFolderPath, Path pluginPlayerDataFolderPath)
  {
    Preconditions.checkNotNull(minecraftPlayerDataFolderPath, "minecraftPlayerDataFolderPath");
    Preconditions.checkNotNull(pluginPlayerDataFolderPath, "pluginPlayerDataFolderPath");

    this.minecraftPlayerDataFolderPath = minecraftPlayerDataFolderPath;
    this.pluginPlayerDataFolderPath = pluginPlayerDataFolderPath;
  }

  public Pair<ReadWriteResult, DeltaEssPlayerFile> read(String playerName)
  {
    ExtraPreconditions.checkNotEmpty(playerName, "playerName");

    try
    {
      Path pathToFile;
      YamlConfiguration configuration;
      byte[] bytes;
      String fileAsString;
      DeltaEssPlayerFile playerFile = new DeltaEssPlayerFile();

      // Start reading Minecraft player data
      pathToFile = getPathToPlayerDataFile(minecraftPlayerDataFolderPath, playerName);

      // Minecraft player data is required
      if (!Files.exists(pathToFile))
      {
        return Pair.of(ReadWriteResult.NOT_FOUND, null);
      }

      bytes = Files.readAllBytes(pathToFile);
      fileAsString = new String(bytes, StandardCharsets.UTF_8);
      configuration = new YamlConfiguration();
      configuration.loadFromString(fileAsString);

      readMinecraftPlayerData(configuration, playerFile);

      // Start reading plugin player data
      pathToFile = getPathToPlayerDataFile(pluginPlayerDataFolderPath, playerName);

      // Plugin player data is not required
      if (!Files.exists(pathToFile))
      {
        return Pair.of(ReadWriteResult.FINISHED, playerFile);
      }

      bytes = Files.readAllBytes(pathToFile);
      fileAsString = new String(bytes, StandardCharsets.UTF_8);
      configuration = new YamlConfiguration();
      configuration.loadFromString(fileAsString);

      readPluginPlayerData(configuration, playerFile);

      return Pair.of(ReadWriteResult.FINISHED, playerFile);
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return Pair.of(ReadWriteResult.IO_EXCEPTION, null);
    }
    catch (InvalidConfigurationException e)
    {
      e.printStackTrace();
      return Pair.of(ReadWriteResult.INVALID_CONFIG, null);
    }
  }

  private static Path getPathToPlayerDataFile(Path folderPath, String playerName) throws IOException
  {
    // Make sure player name is lower cased
    playerName = playerName.toLowerCase();

    String playerNameFirstChar = String.valueOf(playerName.charAt(0));
    Path pathToSubFolder = folderPath.resolve(playerNameFirstChar);

    // Create the subdirectory if it doesn't exist
    Files.createDirectories(pathToSubFolder);

    return pathToSubFolder.resolve(playerName + ".yml");
  }

  private static void readMinecraftPlayerData(
    YamlConfiguration config, DeltaEssPlayerFile playerFile)
  {
    double health = config.getDouble(PlayerDataFileKeys.HEALTH, 20.0);
    int foodLevel = config.getInt(PlayerDataFileKeys.FOOD_LEVEL, 20);
    int xpLevel = config.getInt(PlayerDataFileKeys.XP_LEVEL, 0);
    double xpProgress = config.getDouble(PlayerDataFileKeys.XP_PROGRESS, 0.0);
    String gameMode = config.getString(PlayerDataFileKeys.GAMEMODE, GameMode.SURVIVAL.toString());
    int heldItemSlot = config.getInt(PlayerDataFileKeys.HELD_ITEM_SLOT, 0);

    playerFile.setHealth(health);
    playerFile.setFoodLevel(foodLevel);
    playerFile.setXpLevel(xpLevel);
    playerFile.setXpProgress((float) xpProgress);
    playerFile.setGameMode(parseGameMode(gameMode));
    playerFile.setHeldItemSlot(heldItemSlot);

    List<String> effectList = config.getStringList(PlayerDataFileKeys.EFFECTS);
    playerFile.setPotionEffects(SerializationUtils.toEffectList(effectList));

    ConfigurationSection section;

    section = config.getConfigurationSection(PlayerDataFileKeys.ENDER_CHEST_SECTION);
    playerFile.setEnderChest(SerializationUtils.toItemStackArray(section, 27));

    section = config.getConfigurationSection(PlayerDataFileKeys.SURVIVAL_INV_SECTION);
    playerFile.setSurvival(SerializationUtils.toSavedInventory(section));

    section = config.getConfigurationSection(PlayerDataFileKeys.CREATIVE_INV_SECTION);
    playerFile.setCreative(SerializationUtils.toSavedInventory(section));
  }

  private static void readPluginPlayerData(YamlConfiguration config, DeltaEssPlayerFile playerFile)
  {
    boolean vanished = config.getBoolean(PlayerDataFileKeys.VANISHED, false);
    boolean blockingTeleports = config.getBoolean(PlayerDataFileKeys.BLOCKING_TELEPORTS, false);
    String socialSpyLevel = config.getString(
      PlayerDataFileKeys.SOCIAL_SPY_LEVEL, SocialSpyLevel.NONE.toString());
    String replyingTo = config.getString(PlayerDataFileKeys.REPLYING_TO, "");

    playerFile.setVanished(vanished);
    playerFile.setBlockingTeleports(blockingTeleports);
    playerFile.setSocialSpyLevel(parseSocialSpyLevel(socialSpyLevel));
    playerFile.setReplyingTo(replyingTo);
    playerFile.setPluginPlayerData(config);
  }

  private static GameMode parseGameMode(String input)
  {
    try
    {
      return (input != null) ? GameMode.valueOf(input) : GameMode.SURVIVAL;
    }
    catch (IllegalArgumentException e)
    {
      return GameMode.SURVIVAL;
    }
  }

  private static SocialSpyLevel parseSocialSpyLevel(String input)
  {
    try
    {
      return (input != null) ? SocialSpyLevel.valueOf(input) : SocialSpyLevel.NONE;
    }
    catch (IllegalArgumentException e)
    {
      return SocialSpyLevel.NONE;
    }
  }
}

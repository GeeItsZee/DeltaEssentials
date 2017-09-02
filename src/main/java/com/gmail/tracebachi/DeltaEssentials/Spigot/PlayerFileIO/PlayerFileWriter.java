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
import com.gmail.tracebachi.SockExchange.Utilities.ExtraPreconditions;
import com.google.common.base.Preconditions;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerFileWriter
{
  private final Path minecraftPlayerDataFolderPath;
  private final Path pluginPlayerDataFolderPath;

  public PlayerFileWriter(
    Path minecraftPlayerDataFolderPath, Path pluginPlayerDataFolderPath)
  {
    Preconditions.checkNotNull(minecraftPlayerDataFolderPath, "minecraftPlayerDataFolderPath");
    Preconditions.checkNotNull(pluginPlayerDataFolderPath, "pluginPlayerDataFolderPath");

    this.minecraftPlayerDataFolderPath = minecraftPlayerDataFolderPath;
    this.pluginPlayerDataFolderPath = pluginPlayerDataFolderPath;
  }

  public ReadWriteResult write(String playerName, DeltaEssPlayerFile playerFile)
  {
    ExtraPreconditions.checkNotEmpty(playerName, "playerName");
    Preconditions.checkNotNull(playerFile, "playerFile");

    try
    {
      Path pathToFile;

      pathToFile = getPathToPlayerDataFile(minecraftPlayerDataFolderPath, playerName);

      try (BufferedWriter writer = Files.newBufferedWriter(pathToFile, StandardCharsets.UTF_8))
      {
        String playerData = writeMinecraftPlayerData(playerFile);
        writer.write(playerData);
      }

      pathToFile = getPathToPlayerDataFile(pluginPlayerDataFolderPath, playerName);

      try (BufferedWriter writer = Files.newBufferedWriter(pathToFile, StandardCharsets.UTF_8))
      {
        String playerData = writePluginPlayerData(playerFile);
        writer.write(playerData);
      }

      return ReadWriteResult.FINISHED;
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return ReadWriteResult.IO_EXCEPTION;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return ReadWriteResult.EXCEPTION;
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

  private static String writeMinecraftPlayerData(DeltaEssPlayerFile playerFile)
  {
    YamlConfiguration config = new YamlConfiguration();
    config.set("LastSavedAt", System.currentTimeMillis());

    config.set(PlayerDataFileKeys.HEALTH, playerFile.getHealth());
    config.set(PlayerDataFileKeys.FOOD_LEVEL, playerFile.getFoodLevel());
    config.set(PlayerDataFileKeys.XP_LEVEL, playerFile.getXpLevel());
    config.set(PlayerDataFileKeys.XP_PROGRESS, playerFile.getXpProgress());
    config.set(PlayerDataFileKeys.GAMEMODE, playerFile.getGameMode().toString());
    config.set(PlayerDataFileKeys.HELD_ITEM_SLOT, playerFile.getHeldItemSlot());

    List<String> effectList = SerializationUtils.toStringList(playerFile.getPotionEffects());
    config.set(PlayerDataFileKeys.EFFECTS, effectList);

    config.set(
      PlayerDataFileKeys.ENDER_CHEST_SECTION,
      SerializationUtils.toConfigurationSection(playerFile.getEnderChest()));
    config.set(
      PlayerDataFileKeys.SURVIVAL_INV_SECTION,
      SerializationUtils.toConfigurationSection(playerFile.getSurvival()));
    config.set(
      PlayerDataFileKeys.CREATIVE_INV_SECTION,
      SerializationUtils.toConfigurationSection(playerFile.getCreative()));

    return config.saveToString();
  }

  private static String writePluginPlayerData(DeltaEssPlayerFile playerFile)
  {
    YamlConfiguration config = playerFile.getPluginPlayerData();
    config.set("LastSavedAt", System.currentTimeMillis());

    config.set(PlayerDataFileKeys.BLOCKING_TELEPORTS, playerFile.isBlockingTeleports());
    config.set(PlayerDataFileKeys.VANISHED, playerFile.isVanished());
    config.set(PlayerDataFileKeys.REPLYING_TO, playerFile.getReplyingTo());
    config.set(PlayerDataFileKeys.SOCIAL_SPY_LEVEL, playerFile.getSocialSpyLevel().toString());

    return config.saveToString();
  }
}

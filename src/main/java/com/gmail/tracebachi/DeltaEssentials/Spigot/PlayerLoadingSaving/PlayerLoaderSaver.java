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
package com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.FormatNames;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.PlayerFileReader;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.PlayerFileWriter;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.ReadWriteResult;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving.DeltaEssPlayerFileWrapper.FileState;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.ExtraPreconditions;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Pair;
import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * @author GeeItsZee (tracebachi@gmail.com).
 */
public class PlayerLoaderSaver
{
  private final DeltaEssentialsPlugin plugin;
  private final Map<String, DeltaEssPlayerFileWrapper> playerFileWrapperMap;
  private final PlayerFileLockListener playerFileLockListener;
  private final PlayerFileReader playerFileReader;
  private final PlayerFileWriter playerFileWriter;
  private final PlayerFileApplier playerFileApplier;
  private final List<String> preSaveCommands;
  private final MessageFormatMap formatMap;
  private final BasicLogger basicLogger;

  public PlayerLoaderSaver(
    DeltaEssentialsPlugin plugin, Map<String, DeltaEssPlayerFileWrapper> playerFileWrapperMap,
    PlayerFileLockListener playerFileLockListener, PlayerFileReader playerFileReader,
    PlayerFileWriter playerFileWriter, PlayerFileApplier playerFileApplier,
    List<String> preSaveCommands, MessageFormatMap formatMap, BasicLogger basicLogger)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(playerFileWrapperMap, "playerFileWrapperMap");
    Preconditions.checkNotNull(playerFileLockListener, "playerFileLockListener");
    Preconditions.checkNotNull(playerFileReader, "playerFileReader");
    Preconditions.checkNotNull(playerFileWriter, "playerFileWriter");
    Preconditions.checkNotNull(playerFileApplier, "playerFileApplier");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(preSaveCommands, "preSaveCommands");
    Preconditions.checkNotNull(basicLogger, "basicLogger");

    this.plugin = plugin;
    this.playerFileWrapperMap = playerFileWrapperMap;
    this.playerFileLockListener = playerFileLockListener;
    this.playerFileReader = playerFileReader;
    this.playerFileWriter = playerFileWriter;
    this.playerFileApplier = playerFileApplier;
    this.preSaveCommands = preSaveCommands;
    this.formatMap = formatMap;
    this.basicLogger = basicLogger;
  }

  public void loadPlayer(String nameToLoad, Player requester)
  {
    ExtraPreconditions.checkNotEmpty(nameToLoad, "nameToLoad");
    Preconditions.checkNotNull(requester, "requester");

    DeltaEssPlayerFileWrapper playerFileWrapper = playerFileWrapperMap.get(nameToLoad);

    // If the file wrapper was found, the name cannot be loaded again.
    if (playerFileWrapper != null)
    {
      FileState fileState = playerFileWrapper.getFileState();
      requester.sendMessage(
        formatMap.format(FormatNames.PLAYER_FILE_FAILURE, nameToLoad, "LOAD", fileState));
      return;
    }

    String requesterName = requester.getName();
    boolean loadedByOwner = requesterName.equals(nameToLoad);

    // Put a new player file wrapper for nameToLoad
    playerFileWrapper = new DeltaEssPlayerFileWrapper(loadedByOwner);
    playerFileWrapperMap.put(nameToLoad, playerFileWrapper);

    // Request the lock and read the file if the request was successful
    playerFileLockListener.requestLock(nameToLoad, (hasLock) ->
    {
      if (!hasLock)
      {
        plugin.executeSync(() -> onReadResult(nameToLoad, requesterName, false, null, null));
        return;
      }

      Pair<ReadWriteResult, DeltaEssPlayerFile> pair = playerFileReader.read(nameToLoad);
      ReadWriteResult readResult = pair.getLeft();
      DeltaEssPlayerFile playerFile = pair.getRight();

      plugin.executeSync(
        () -> onReadResult(nameToLoad, requesterName, true, readResult, playerFile));
    });
  }

  public void savePlayer(String nameToSave, Player requester)
  {
    ExtraPreconditions.checkNotEmpty(nameToSave, "nameToSave");
    Preconditions.checkNotNull(requester, "requester");

    DeltaEssPlayerFileWrapper playerFileWrapper = playerFileWrapperMap.get(nameToSave);

    // If the player has no data, they cannot be saved.
    if (playerFileWrapper == null)
    {
      String message = formatMap.format(
        FormatNames.PLAYER_FILE_FAILURE, nameToSave, "SAVE", "NO_DATA");
      requester.sendMessage(message);
      return;
    }

    // If the player is not loaded, they cannot be saved.
    FileState state = playerFileWrapper.getFileState();
    if (state != FileState.LOADED)
    {
      String message = formatMap.format(
        FormatNames.PLAYER_FILE_FAILURE, nameToSave, "SAVE", state);
      requester.sendMessage(message);
      return;
    }

    DeltaEssPlayerFile deltaEssPlayerFile = playerFileWrapper.getPlayerFile();
    String requesterName = requester.getName();

    if (!requesterName.equals(nameToSave))
    {
      // If the data was loaded by the owner of the file, do not allow the save.
      // The file can only be saved by someone else since someone else loaded it.
      if (playerFileWrapper.wasLoadedByOwner())
      {
        String message = formatMap.format(
          FormatNames.PLAYER_FILE_FAILURE, nameToSave, "SAVE", "LOADED_BY_OWNER");
        requester.sendMessage(message);
        return;
      }
    }
    else
    {
      // If the data was not loaded by the owner of the file, do not allow the save.
      // The file can only be saved by the owner since the owner loaded it.
      if (!playerFileWrapper.wasLoadedByOwner())
      {
        String message = formatMap.format(
          FormatNames.PLAYER_FILE_FAILURE, nameToSave, "SAVE", "LOADED_BY_OTHER");
        requester.sendMessage(message);
        return;
      }

      // Have the player run the pre-save commands
      for (String command : preSaveCommands)
      {
        requester.performCommand(command);
      }

      // Call the pre save event
      PlayerPreSaveEvent preSaveEvent = new PlayerPreSaveEvent(
        requester, deltaEssPlayerFile.getPluginPlayerData());
      plugin.callEvent(preSaveEvent);

      // Update player file with how the player exists after the pre-save event
      playerFileApplier.updateFrom(deltaEssPlayerFile, requester);
    }

    // Switch the state to saving
    playerFileWrapper.setFileState(FileState.SAVE_IN_PROGRESS);

    // Request the lock. Write to the file in any case, but the server should have the lock.
    playerFileLockListener.requestLock(nameToSave, (hadLock) ->
    {
      ReadWriteResult writeResult = playerFileWriter.write(nameToSave, deltaEssPlayerFile);
      plugin.executeSync(() -> onWriteResult(nameToSave, requesterName, hadLock, writeResult));
    });
  }

  public void savePlayerForShutdown(Player playerToSave)
  {
    Preconditions.checkNotNull(playerToSave, "playerToSave");

    String nameToSave = playerToSave.getName();
    DeltaEssPlayerFileWrapper playerFileWrapper = playerFileWrapperMap.get(nameToSave);

    // If the player has no data or is not loaded, they cannot be saved.
    if (playerFileWrapper == null || playerFileWrapper.getFileState() != FileState.LOADED)
    {
      return;
    }

    DeltaEssPlayerFile deltaEssPlayerFile = playerFileWrapper.getPlayerFile();

    // If the data was not loaded by the owner of the file, do not allow the save.
    // The file can only be saved by the owner since the owner loaded it.
    if (!playerFileWrapper.wasLoadedByOwner())
    {
      return;
    }

    // Have the player run the pre-save commands
    for (String command : preSaveCommands)
    {
      playerToSave.performCommand(command);
    }

    // Call the pre save event
    PlayerPreSaveEvent preSaveEvent = new PlayerPreSaveEvent(
      playerToSave, deltaEssPlayerFile.getPluginPlayerData());
    plugin.callEvent(preSaveEvent);

    // Update player file with how the player exists after the pre-save event
    playerFileApplier.updateFrom(deltaEssPlayerFile, playerToSave);

    // Switch the state to saving
    playerFileWrapper.setFileState(FileState.SAVE_IN_PROGRESS);

    // Write synchronously and without any checks for locks. Since this method should only
    // be called on shutdown, we can't wait to check locks.
    playerFileWriter.write(nameToSave, deltaEssPlayerFile);

    // Release the lock and remove from player map
    releaseLockAndRemoveFromMap(nameToSave);
  }

  private void releaseLockAndRemoveFromMap(String name)
  {
    playerFileLockListener.releaseLock(name);
    playerFileWrapperMap.remove(name);
  }

  private void onReadResult(
    String nameToLoad, String requesterName, boolean hasLock, ReadWriteResult readResult,
    DeltaEssPlayerFile playerFile)
  {
    basicLogger.debug(
      "[PlayerLoaderSaver.onReadResult]: NameToLoad: '%s'. RequesterName: '%s'. HasLock: '%s'. ReadResult: '%s'.",
      nameToLoad, requesterName, hasLock, readResult);

    DeltaEssPlayerFileWrapper playerFileWrapper = playerFileWrapperMap.get(nameToLoad);
    Player requester = plugin.getServer().getPlayerExact(requesterName);

    // If the player file wrapper does not exist anymore or the requester is not online,
    // there is no need for the lock or file.
    if (playerFileWrapper == null || requester == null)
    {
      releaseLockAndRemoveFromMap(nameToLoad);
      return;
    }

    // If the lock request failed, there is no need for the lock or file.
    if (!hasLock)
    {
      releaseLockAndRemoveFromMap(nameToLoad);

      String message = formatMap.format(
        FormatNames.PLAYER_FILE_FAILURE, nameToLoad, "LOAD", "LOCK_REQUEST_FAILED");
      requester.sendMessage(message);
      return;
    }

    if (!requesterName.equals(nameToLoad))
    {
      // If the file read did not finish, there is no need for the lock or file.
      if (readResult != ReadWriteResult.FINISHED)
      {
        releaseLockAndRemoveFromMap(nameToLoad);

        String message = formatMap.format(
          FormatNames.PLAYER_FILE_FAILURE, nameToLoad, "LOAD", readResult);
        requester.sendMessage(message);
      }
      else
      {
        // Just set the player file and mark as loaded
        playerFileWrapper.setPlayerFile(playerFile);
        playerFileWrapper.setFileState(FileState.LOADED);

        String message = formatMap.format(
          FormatNames.PLAYER_FILE_SUCCESS, nameToLoad, "LOADED");
        requester.sendMessage(message);
      }

      return;
    }

    // If the file read did not finish or the file was not found,
    // there is no need for the lock or file.
    if (readResult != ReadWriteResult.FINISHED && readResult != ReadWriteResult.NOT_FOUND)
    {
      releaseLockAndRemoveFromMap(nameToLoad);

      String message = formatMap.format(
        FormatNames.PLAYER_FILE_FAILURE, nameToLoad, "LOAD", readResult);
      requester.sendMessage(message);
      return;
    }

    // If the player being loaded did not have a player file, create one based on
    // how the player is currently. (Requester == Player to load)
    if (readResult == ReadWriteResult.NOT_FOUND)
    {
      playerFile = new DeltaEssPlayerFile();
      playerFileApplier.updateFrom(playerFile, requester);
    }

    // Apply the file
    playerFileWrapper.setPlayerFile(playerFile);
    playerFileApplier.applyTo(playerFile, requester);

    // Switch the state to loaded
    playerFileWrapper.setFileState(FileState.LOADED);

    // Call the post load event
    PlayerPostLoadEvent postLoadEvent = new PlayerPostLoadEvent(requester,
      playerFile.getPluginPlayerData(), readResult == ReadWriteResult.NOT_FOUND);
    plugin.callEvent(postLoadEvent);
  }

  private void onWriteResult(
    String nameToSave, String requesterName, boolean hadLock, ReadWriteResult writeResult)
  {
    basicLogger.debug(
      "[PlayerLoaderSaver.onWriteResult]: NameToSave: '%s'. RequesterName: '%s'. HadLock: '%s'. WriteResult: '%s'.",
      nameToSave, requesterName, hadLock, writeResult);

    releaseLockAndRemoveFromMap(nameToSave);

    // If the requester is not the same as the name being saved, send a message about the result.
    if (!nameToSave.equals(requesterName))
    {
      String message;
      Player requester = plugin.getServer().getPlayerExact(requesterName);

      if (requester == null)
      {
        return;
      }

      if (writeResult == ReadWriteResult.FINISHED)
      {
        message = formatMap.format(
          FormatNames.PLAYER_FILE_SUCCESS, nameToSave, "SAVED");
        requester.sendMessage(message);
      }
      else
      {
        message = formatMap.format(
          FormatNames.PLAYER_FILE_FAILURE, nameToSave, "SAVE", writeResult);
        requester.sendMessage(message);
      }
    }
  }
}

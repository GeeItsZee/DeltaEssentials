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

import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.google.common.base.Preconditions;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class DeltaEssPlayerFileWrapper
{
  public enum FileState
  {
    LOAD_IN_PROGRESS,
    LOADED,
    SAVE_IN_PROGRESS
  }

  private final boolean loadedByOwner;
  private FileState fileState;
  private DeltaEssPlayerFile playerFile;

  public DeltaEssPlayerFileWrapper(boolean loadedByOwner)
  {
    this.loadedByOwner = loadedByOwner;
    this.fileState = FileState.LOAD_IN_PROGRESS;
  }

  public boolean wasLoadedByOwner()
  {
    return loadedByOwner;
  }

  public FileState getFileState()
  {
    return fileState;
  }

  public void setFileState(FileState fileState)
  {
    Preconditions.checkNotNull(fileState, "fileState");
    this.fileState = fileState;
  }

  public DeltaEssPlayerFile getPlayerFile()
  {
    return playerFile;
  }

  public void setPlayerFile(DeltaEssPlayerFile playerFile)
  {
    this.playerFile = playerFile;
  }
}

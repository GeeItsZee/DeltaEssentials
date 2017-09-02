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

import com.gmail.tracebachi.DeltaEssentials.Spigot.Chat.*;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileEditing.PlayerFileCommand;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileEditing.PlayerFileInventoryCloseListener;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.PlayerFileReader;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.PlayerFileWriter;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving.*;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving.DeltaEssPlayerFileWrapper.FileState;
import com.gmail.tracebachi.DeltaEssentials.Spigot.Teleport.*;
import com.gmail.tracebachi.DeltaEssentials.Spigot.Vanish.DVanishCommand;
import com.gmail.tracebachi.DeltaEssentials.Spigot.Vanish.FindPlayerCommand;
import com.gmail.tracebachi.SockExchange.Scheduler.AwaitableExecutor;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.CaseInsensitiveMap;
import com.gmail.tracebachi.SockExchange.Utilities.JulBasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class DeltaEssentialsPlugin extends JavaPlugin
{
  private volatile boolean canScheduleSyncTasks = false;
  private DeltaEssentialsConfiguration config;
  private Map<String, DeltaEssPlayerFileWrapper> playerFileWrapperMap;
  private BasicLogger basicLogger;
  private AwaitableExecutor awaitableExecutor;

  private PlayerFileLockListener playerFileLockListener;
  private PlayerFileReader playerFileReader;
  private PlayerFileWriter playerFileWriter;
  private PlayerFileApplier playerFileApplier;
  private PlayerLoaderSaver playerLoaderSaver;
  private PlayerJoinQuitListener playerJoinQuitListener;
  private FirstJoinListener firstJoinListener;
  private GameModeChangeListener gameModeChangeListener;
  private PlayerActionListener playerActionListener;
  private SocialSpyListener socialSpyListener;
  private TellChatManager tellChatManager;
  private SameServerTeleporter sameServerTeleporter;
  private TpAfterLoadListener tpAfterLoadListener;
  private TpHereManager tpHereManager;
  private TpToManager tpToManager;
  private PlayerFileInventoryCloseListener inventoryCloseListener;

  private ReplyCommand replyCommand;
  private SocialSpyCommand socialSpyCommand;
  private TellCommand tellCommand;
  private PlayerFileCommand playerFileCommand;
  private BlockTpCommand blockTpCommand;
  private TpAcceptCommand tpAcceptCommand;
  private TpAskHereCommand tpAskHereCommand;
  private TpCommand tpCommand;
  private TpDenyCommand tpDenyCommand;
  private TpHereCommand tpHereCommand;
  private DisposalCommand disposalCommand;
  private DVanishCommand dVanishCommand;
  private FindPlayerCommand findPlayerCommand;

  @Override
  public void onLoad()
  {
    saveDefaultConfig();
  }

  @Override
  public void onEnable()
  {
    canScheduleSyncTasks = true;

    reloadConfig();

    config = new DeltaEssentialsConfiguration();
    config.read(getConfig());

    SockExchangeApi api = SockExchangeApi.instance();
    MessageFormatMap messageFormatMap = config.getFormatMap();
    Path minecraftPlayerDataFolderPath = getDataFolder().toPath().resolve("MinecraftPlayerData");
    Path pluginPlayerDataFolderPath = getDataFolder().toPath().resolve("PluginPlayerData");

    playerFileWrapperMap = new CaseInsensitiveMap<>(new HashMap<>());
    basicLogger = new JulBasicLogger(getLogger(), config.inDebugMode());
    awaitableExecutor = new AwaitableExecutor(api.getScheduledExecutorService());

    playerFileLockListener = new PlayerFileLockListener(api, awaitableExecutor);
    playerFileLockListener.register();

    playerFileReader = new PlayerFileReader(
      minecraftPlayerDataFolderPath, pluginPlayerDataFolderPath);

    playerFileWriter = new PlayerFileWriter(
      minecraftPlayerDataFolderPath, pluginPlayerDataFolderPath);

    playerFileApplier = new PlayerFileApplier(config.shouldLoadAndSavePotionEffects(),
      config.shouldForceDefaultGameMode(), config.getDefaultGameMode());

    playerLoaderSaver = new PlayerLoaderSaver(this, playerFileWrapperMap, playerFileLockListener,
      playerFileReader, playerFileWriter, playerFileApplier, config.getPreSaveCommands(),
      messageFormatMap, basicLogger);

    playerJoinQuitListener = new PlayerJoinQuitListener(this, playerLoaderSaver);
    playerJoinQuitListener.register();

    firstJoinListener = new FirstJoinListener(this, config.getFirstJoinItemStacks());
    firstJoinListener.register();

    gameModeChangeListener = new GameModeChangeListener(this, config.getBlockedGameModes(), messageFormatMap);
    gameModeChangeListener.register();

    playerActionListener = new PlayerActionListener(this, playerFileWrapperMap, messageFormatMap);
    playerActionListener.register();

    socialSpyListener = new SocialSpyListener(this, messageFormatMap, api);
    socialSpyListener.register();

    tellChatManager = new TellChatManager(this, socialSpyListener, messageFormatMap, basicLogger, api);
    tellChatManager.register();

    sameServerTeleporter = new SameServerTeleporter(this, messageFormatMap, api);

    tpAfterLoadListener = new TpAfterLoadListener(this, sameServerTeleporter, messageFormatMap, api);
    tpAfterLoadListener.register();

    tpHereManager = new TpHereManager(this, sameServerTeleporter, messageFormatMap, api);
    tpHereManager.register();

    tpToManager = new TpToManager(this, tpAfterLoadListener, sameServerTeleporter, messageFormatMap, api);
    tpToManager.register();

    inventoryCloseListener = new PlayerFileInventoryCloseListener(this);
    inventoryCloseListener.register();

    replyCommand = new ReplyCommand(this, tellChatManager, messageFormatMap, api);
    replyCommand.register();

    socialSpyCommand = new SocialSpyCommand(this, messageFormatMap);
    socialSpyCommand.register();

    tellCommand = new TellCommand(this, tellChatManager, messageFormatMap, api);
    tellCommand.register();

    playerFileCommand = new PlayerFileCommand(this, playerLoaderSaver, playerFileWrapperMap,
      messageFormatMap);
    playerFileCommand.register();

    blockTpCommand = new BlockTpCommand(this, messageFormatMap);
    blockTpCommand.register();

    tpAcceptCommand = new TpAcceptCommand(this, tpHereManager, messageFormatMap);
    tpAcceptCommand.register();

    tpAskHereCommand = new TpAskHereCommand(this, tpHereManager, messageFormatMap, api);
    tpAskHereCommand.register();

    tpCommand = new TpCommand(this, tpToManager, messageFormatMap, api);
    tpCommand.register();

    tpDenyCommand = new TpDenyCommand(this, tpHereManager, messageFormatMap);
    tpDenyCommand.register();

    tpHereCommand = new TpHereCommand(this, tpHereManager, messageFormatMap, api);
    tpHereCommand.register();

    disposalCommand = new DisposalCommand(this, messageFormatMap);
    disposalCommand.register();

    dVanishCommand = new DVanishCommand(this, messageFormatMap);
    dVanishCommand.register();

    findPlayerCommand = new FindPlayerCommand(this, messageFormatMap, api);
    findPlayerCommand.register();
  }

  @Override
  public void onDisable()
  {
    canScheduleSyncTasks = false;

    // Save all online players synchronously
    saveOnlinePlayers();

    if (findPlayerCommand != null)
    {
      findPlayerCommand.unregister();
      findPlayerCommand = null;
    }

    if (dVanishCommand != null)
    {
      dVanishCommand.unregister();
      dVanishCommand = null;
    }

    if (disposalCommand != null)
    {
      disposalCommand.unregister();
      disposalCommand = null;
    }

    if (tpHereCommand != null)
    {
      tpHereCommand.unregister();
      tpHereCommand = null;
    }

    if (tpDenyCommand != null)
    {
      tpDenyCommand.unregister();
      tpDenyCommand = null;
    }

    if (tpCommand != null)
    {
      tpCommand.unregister();
      tpCommand = null;
    }

    if (tpAskHereCommand != null)
    {
      tpAskHereCommand.unregister();
      tpAskHereCommand = null;
    }

    if (tpAcceptCommand != null)
    {
      tpAcceptCommand.unregister();
      tpAcceptCommand = null;
    }

    if (blockTpCommand != null)
    {
      blockTpCommand.unregister();
      blockTpCommand = null;
    }

    if (playerFileCommand != null)
    {
      playerFileCommand.unregister();
      playerFileCommand = null;
    }

    if (tellCommand != null)
    {
      tellCommand.unregister();
      tellCommand = null;
    }

    if (socialSpyCommand != null)
    {
      socialSpyCommand.unregister();
      socialSpyCommand = null;
    }

    if (replyCommand != null)
    {
      replyCommand.unregister();
      replyCommand = null;
    }

    if (inventoryCloseListener != null)
    {
      inventoryCloseListener.unregister();
      inventoryCloseListener = null;
    }

    if (tpToManager != null)
    {
      tpToManager.unregister();
      tpToManager = null;
    }

    if (tpHereManager != null)
    {
      tpHereManager.unregister();
      tpHereManager = null;
    }

    if (tpAfterLoadListener != null)
    {
      tpAfterLoadListener.unregister();
      tpAfterLoadListener = null;
    }

    sameServerTeleporter = null;

    if (tellChatManager != null)
    {
      tellChatManager.unregister();
      tellChatManager = null;
    }

    if (socialSpyListener != null)
    {
      socialSpyListener.unregister();
      socialSpyListener = null;
    }

    if (playerActionListener != null)
    {
      playerActionListener.unregister();
      playerActionListener = null;
    }

    if (gameModeChangeListener != null)
    {
      gameModeChangeListener.unregister();
      gameModeChangeListener = null;
    }

    if (firstJoinListener != null)
    {
      firstJoinListener.unregister();
      firstJoinListener = null;
    }

    if (playerJoinQuitListener != null)
    {
      playerJoinQuitListener.unregister();
      playerJoinQuitListener = null;
    }

    playerLoaderSaver = null;
    playerFileApplier = null;
    playerFileWriter = null;
    playerFileReader = null;

    if (playerFileLockListener != null)
    {
      playerFileLockListener.unregister();
      playerFileLockListener = null;
    }

    if (awaitableExecutor != null)
    {
      try
      {
        awaitableExecutor.setAcceptingTasks(false);
        awaitableExecutor.awaitTasksWithSleep(10, 1000);
        awaitableExecutor.shutdown();
      }
      catch (InterruptedException ex)
      {
        ex.printStackTrace();
      }

      awaitableExecutor = null;
    }

    basicLogger = null;
    playerFileWrapperMap = null;
    config = null;
  }

  public void executeSync(Runnable runnable)
  {
    try
    {
      if (canScheduleSyncTasks)
      {
        getServer().getScheduler().runTask(this, runnable);
        return;
      }
    }
    catch (IllegalPluginAccessException ex)
    {
      if (!ex.getMessage().equals("Plugin attempted to register task while disabled"))
      {
        throw ex;
      }
    }

    // If we can't schedule or the correct exception is thrown, run the runnable now.
    try
    {
      runnable.run();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  public void callEvent(Event event)
  {
    getServer().getPluginManager().callEvent(event);
  }

  public <T extends DelayedHandingEvent> void callDelayedHandlingEvent(DelayedHandingEvent<T> event)
  {
    getServer().getPluginManager().callEvent(event);
    event.completeIntent();
  }

  public DeltaEssPlayerFile getLoadedPlayerFile(String name)
  {
    DeltaEssPlayerFileWrapper playerFileWrapper = playerFileWrapperMap.get(name);

    if (playerFileWrapper == null || playerFileWrapper.getFileState() != FileState.LOADED)
    {
      return null;
    }

    return playerFileWrapper.getPlayerFile();
  }

  public void forEachLoadedPlayerFile(BiConsumer<String, DeltaEssPlayerFile> biConsumer)
  {
    for (Map.Entry<String, DeltaEssPlayerFileWrapper> entry : playerFileWrapperMap.entrySet())
    {
      DeltaEssPlayerFileWrapper playerFileWrapper = entry.getValue();

      if (playerFileWrapper == null || playerFileWrapper.getFileState() != FileState.LOADED)
      {
        continue;
      }

      biConsumer.accept(entry.getKey(), playerFileWrapper.getPlayerFile());
    }
  }

  private void saveOnlinePlayers()
  {
    for (Player playerToSave : getServer().getOnlinePlayers())
    {
      try
      {
        playerLoaderSaver.savePlayerForShutdown(playerToSave);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}

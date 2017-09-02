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
package com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileEditing;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.FormatNames;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.SavedPlayerInventory;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving.DeltaEssPlayerFileWrapper;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving.DeltaEssPlayerFileWrapper.FileState;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving.PlayerLoaderSaver;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerFileCommand implements CommandExecutor, Registerable
{
  private static final String COMMAND_NAME = "playerfile";
  private static final String COMMAND_USAGE = "/playerfile <load|open|save> <name>";
  private static final String COMMAND_USAGE_OPEN = "/playerfile open <name> <survival|creative|enderchest>";
  private static final String COMMAND_PERM = "DeltaEss.PlayerFile";

  private final DeltaEssentialsPlugin plugin;
  private final PlayerLoaderSaver playerLoaderSaver;
  private final Map<String, DeltaEssPlayerFileWrapper> playerFileWrapperMap;
  private final MessageFormatMap formatMap;

  public PlayerFileCommand(
    DeltaEssentialsPlugin plugin, PlayerLoaderSaver playerLoaderSaver,
    Map<String, DeltaEssPlayerFileWrapper> playerFileWrapperMap, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(playerLoaderSaver, "playerLoaderSaver");
    Preconditions.checkNotNull(playerFileWrapperMap, "playerFileWrapperMap");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.plugin = plugin;
    this.playerLoaderSaver = playerLoaderSaver;
    this.playerFileWrapperMap = playerFileWrapperMap;
    this.formatMap = formatMap;
  }

  @Override
  public void register()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(this);
  }

  @Override
  public void unregister()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(null);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (!(sender instanceof Player))
    {
      sender.sendMessage(formatMap.format(FormatNames.PLAYER_ONLY_COMMAND, COMMAND_NAME));
      return true;
    }

    if (!sender.hasPermission(COMMAND_PERM))
    {
      sender.sendMessage(formatMap.format(FormatNames.NO_PERM, COMMAND_PERM));
      return true;
    }

    if (args.length < 2)
    {
      sender.sendMessage(formatMap.format(FormatNames.USAGE, COMMAND_USAGE));
      return true;
    }

    String otherPlayersName = args[1];

    if (args[0].equalsIgnoreCase("load"))
    {
      playerLoaderSaver.loadPlayer(otherPlayersName, (Player) sender);
    }
    else if (args[0].equalsIgnoreCase("save"))
    {
      playerLoaderSaver.savePlayer(otherPlayersName, (Player) sender);
    }
    else if (args[0].equalsIgnoreCase("open"))
    {
      String inventoryTypeStr = (args.length >= 3) ? args[2] : "survival";
      openPlayerFile(sender, otherPlayersName, inventoryTypeStr);
    }
    else
    {
      sender.sendMessage(formatMap.format(FormatNames.USAGE, COMMAND_USAGE));
    }

    return true;
  }

  private void openPlayerFile(
    CommandSender sender, String otherPlayersName, String inventoryTypeStr)
  {
    DeltaEssPlayerFileWrapper deltaEssPlayer = playerFileWrapperMap.get(otherPlayersName);

    if (deltaEssPlayer == null || deltaEssPlayer.getFileState() != FileState.LOADED)
    {
      String message = formatMap.format(
        FormatNames.PLAYER_FILE_FAILURE, otherPlayersName, "OPEN", "NOT_LOADED");
      sender.sendMessage(message);
      return;
    }

    if (deltaEssPlayer.wasLoadedByOwner())
    {
      String message = formatMap.format(
        FormatNames.PLAYER_FILE_FAILURE, otherPlayersName, "OPEN", "LOADED_BY_OWNER");
      sender.sendMessage(message);
      return;
    }

    DeltaEssPlayerFile playerFile = deltaEssPlayer.getPlayerFile();
    String inventoryType = inventoryTypeStr.toLowerCase();
    Player player = (Player) sender;

    if (inventoryType.startsWith("s"))
    {
      Inventory inventory = createInventoryToShowSavedInventory(
        otherPlayersName, GameMode.SURVIVAL, playerFile.getSurvival());
      player.openInventory(inventory);
    }
    else if (inventoryType.startsWith("c"))
    {
      Inventory inventory = createInventoryToShowSavedInventory(
        otherPlayersName, GameMode.CREATIVE, playerFile.getCreative());
      player.openInventory(inventory);
    }
    else if (inventoryType.startsWith("e"))
    {
      Inventory inventory = createInventoryToShowEnderChest(
        otherPlayersName, playerFile.getEnderChest());
      player.openInventory(inventory);
    }
    else
    {
      sender.sendMessage(formatMap.format(FormatNames.USAGE, COMMAND_USAGE_OPEN));
    }
  }

  private Inventory createInventoryToShowEnderChest(String ownerName, ItemStack[] itemStacks)
  {
    PlayerFileInventoryHolder holder = new PlayerFileInventoryHolder(
      ownerName, PlayerFileInventoryHolder.Type.ENDER_CHEST);
    Inventory inventory = plugin.getServer().createInventory(
      holder, 9 * 3, "EnderChest - " + ownerName);

    for (int i = 0; i < itemStacks.length; i++)
    {
      inventory.setItem(i, itemStacks[i]);
    }

    return inventory;
  }

  private Inventory createInventoryToShowSavedInventory(
    String ownerName, GameMode gameMode, SavedPlayerInventory savedPlayerInventory)
  {
    ItemStack[] itemStacks;
    Inventory inventory;

    if (gameMode == GameMode.SURVIVAL)
    {
      PlayerFileInventoryHolder holder = new PlayerFileInventoryHolder(
        ownerName, PlayerFileInventoryHolder.Type.SURVIVAL);
      inventory = plugin.getServer().createInventory(holder, 9 * 6, "Survival - " + ownerName);
    }
    else if (gameMode == GameMode.CREATIVE)
    {
      PlayerFileInventoryHolder holder = new PlayerFileInventoryHolder(
        ownerName, PlayerFileInventoryHolder.Type.CREATIVE);
      inventory = plugin.getServer().createInventory(holder, 9 * 6, "Creative - " + ownerName);
    }
    else
    {
      throw new IllegalStateException(String.valueOf(gameMode));
    }

    itemStacks = savedPlayerInventory.getStorage();
    for (int i = 0; i < itemStacks.length; i++)
    {
      inventory.setItem(i, itemStacks[i]);
    }

    itemStacks = savedPlayerInventory.getArmor();
    for (int i = 0; i < itemStacks.length; i++)
    {
      inventory.setItem(i + 36, itemStacks[i]);
    }
    for (int i = 0; i < 5; i++)
    {
      inventory.setItem(i + 40, new ItemStack(Material.LADDER));
    }

    itemStacks = savedPlayerInventory.getExtraSlots();
    for (int i = 0; i < itemStacks.length; i++)
    {
      inventory.setItem(i + 45, itemStacks[i]);
    }
    for (int i = 0; i < 8; i++)
    {
      inventory.setItem(i + 46, new ItemStack(Material.LADDER));
    }

    return inventory;
  }
}

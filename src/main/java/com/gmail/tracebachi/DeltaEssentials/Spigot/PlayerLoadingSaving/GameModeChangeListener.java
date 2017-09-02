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
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Permissions;
import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.DeltaEssPlayerFile;
import com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO.SavedPlayerInventory;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Set;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class GameModeChangeListener implements Listener, Registerable
{
  private final DeltaEssentialsPlugin plugin;
  private final Set<GameMode> blockedGameModes;
  private final MessageFormatMap formatMap;

  public GameModeChangeListener(
    DeltaEssentialsPlugin plugin, Set<GameMode> blockedGameModes, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(blockedGameModes, "blockedGameModes");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.plugin = plugin;
    this.blockedGameModes = blockedGameModes;
    this.formatMap = formatMap;
  }

  @Override
  public void register()
  {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void unregister()
  {
    HandlerList.unregisterAll(this);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
  {
    Player player = event.getPlayer();
    GameMode currentMode = player.getGameMode();
    GameMode newMode = event.getNewGameMode();

    // Ignore changes to the same GameMode
    if (currentMode == newMode)
    {
      return;
    }

    // If the new GameMode is blocked and the player does not have permission to
    // go to that GameMode, prevent the GameMode change.
    String gameModePerm = Permissions.GAMEMODE_PERM_PREFIX + newMode.name();
    if (blockedGameModes.contains(newMode) && !player.hasPermission(gameModePerm))
    {
      player.sendMessage(formatMap.format(FormatNames.NO_PERM, gameModePerm));
      event.setCancelled(true);
      return;
    }

    // If the player is allowed to share inventories across GameModes,
    // ignore the GameMode change.
    if (player.hasPermission(Permissions.GAMEMODE_SHARED_INV))
    {
      return;
    }

    DeltaEssPlayerFile playerFile = plugin.getLoadedPlayerFile(player.getName());

    // If the player does not have a loaded player file, ignore the GameMode change.
    if (playerFile == null)
    {
      return;
    }

    // Save the inventory associated with the old GameMode
    if (currentMode == GameMode.SURVIVAL)
    {
      playerFile.setSurvival(new SavedPlayerInventory(player));
    }
    else if (currentMode == GameMode.CREATIVE)
    {
      playerFile.setCreative(new SavedPlayerInventory(player));
    }

    // Load the inventory associated with the new GameMode
    PlayerInventory playerInventory = player.getInventory();
    if (newMode == GameMode.SURVIVAL)
    {
      SavedPlayerInventory savedSurvival = playerFile.getSurvival();
      playerInventory.setStorageContents(savedSurvival.getStorage());
      playerInventory.setArmorContents(savedSurvival.getArmor());
      playerInventory.setExtraContents(savedSurvival.getExtraSlots());

      playerFile.setSurvival(null);
    }
    else if (newMode == GameMode.CREATIVE)
    {
      SavedPlayerInventory savedCreative = playerFile.getCreative();
      playerInventory.setStorageContents(savedCreative.getStorage());
      playerInventory.setArmorContents(savedCreative.getArmor());
      playerInventory.setExtraContents(savedCreative.getExtraSlots());

      playerFile.setCreative(null);
    }
    else
    {
      playerInventory.clear();
      playerInventory.setArmorContents(new ItemStack[4]);
      playerInventory.setExtraContents(new ItemStack[1]);
    }
  }
}

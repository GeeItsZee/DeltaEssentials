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
package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashSet;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/20/16.
 */
public class PlayerLockListener extends DeltaEssentialsListener
{
    private CaseInsensitiveHashSet locked = new CaseInsensitiveHashSet();

    public PlayerLockListener(DeltaEssentials plugin)
    {
        super(plugin);
    }

    @Override
    public void shutdown()
    {
        locked.clear();
        locked = null;
        super.shutdown();
    }

    public boolean add(String name)
    {
        return locked.add(name);
    }

    public boolean remove(String name)
    {
        return locked.remove(name);
    }

    public boolean isLocked(String name)
    {
        return locked.contains(name);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        HumanEntity player = event.getPlayer();

        if(locked.contains(player.getName()))
        {
            player.sendMessage(Prefixes.FAILURE +
                "You are locked. Please wait until your data is loaded.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryInteract(InventoryInteractEvent event)
    {
        HumanEntity player = event.getWhoClicked();

        if(locked.contains(player.getName()))
        {
            player.sendMessage(Prefixes.FAILURE +
                "You are locked. Please wait until your data is loaded.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItemEvent(PlayerDropItemEvent event)
    {
        Player player = event.getPlayer();

        if(locked.contains(player.getName()))
        {
            player.sendMessage(Prefixes.FAILURE +
                "You are locked. Please wait until your data is loaded.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerPickupItemEvent(PlayerPickupItemEvent event)
    {
        Player player = event.getPlayer();

        if(locked.contains(player.getName()))
        {
            player.sendMessage(Prefixes.FAILURE +
                "You are locked. Please wait until your data is loaded.");
            event.setCancelled(true);
        }
    }
}

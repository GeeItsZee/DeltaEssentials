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
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/20/16.
 */
public class PlayerLockManager extends DeltaEssentialsListener
{
    private CaseInsensitiveHashMap<Long> locked = new CaseInsensitiveHashMap<>();
    private BukkitTask cleanupTask;

    public PlayerLockManager(DeltaEssentials plugin)
    {
        super(plugin);

        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            this::cleanup,
            20,
            20);
    }

    @Override
    public void shutdown()
    {
        cleanupTask.cancel();
        cleanupTask = null;
        locked.clear();
        locked = null;
        super.shutdown();
    }

    public void add(String name)
    {
        add(name, Long.MAX_VALUE);
    }

    public void add(String name, long endTime)
    {
        locked.put(name, endTime);
    }

    public Long remove(String name)
    {
        return locked.remove(name);
    }

    public boolean isLocked(String name)
    {
        return locked.containsKey(name);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        HumanEntity player = event.getPlayer();

        if(locked.containsKey(player.getName()))
        {
            player.sendMessage(Settings.format("PlayerLocked"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryInteract(InventoryInteractEvent event)
    {
        HumanEntity player = event.getWhoClicked();

        if(locked.containsKey(player.getName()))
        {
            player.sendMessage(Settings.format("PlayerLocked"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();

        if(locked.containsKey(player.getName()))
        {
            player.sendMessage(Settings.format("PlayerLocked"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItemEvent(PlayerDropItemEvent event)
    {
        Player player = event.getPlayer();

        if(locked.containsKey(player.getName()))
        {
            player.sendMessage(Settings.format("PlayerLocked"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerPickupItemEvent(PlayerPickupItemEvent event)
    {
        Player player = event.getPlayer();

        if(locked.containsKey(player.getName()))
        {
            player.sendMessage(Settings.format("PlayerLocked"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDamagedEvent(EntityDamageEvent event)
    {
        if(!(event.getEntity() instanceof Player)) { return; }

        Player player = (Player) event.getEntity();

        if(locked.containsKey(player.getName()))
        {
            player.sendMessage(Settings.format("PlayerLocked"));
            event.setCancelled(true);
        }
    }

    private void cleanup()
    {
        Iterator<Map.Entry<String, Long>> iter = locked.entrySet().iterator();
        Long current = System.currentTimeMillis();

        while(iter.hasNext())
        {
            Map.Entry<String, Long> entry = iter.next();

            if(entry.getValue() < current)
            {
                iter.remove();
            }
        }
    }
}

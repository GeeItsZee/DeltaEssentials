package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashSet;
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
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
public class InventoryLockListener extends DeltaEssentialsListener
{
    private CaseInsensitiveHashSet locked = new CaseInsensitiveHashSet();

    public InventoryLockListener(DeltaEssentials plugin)
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
            player.sendMessage(Prefixes.INFO +
                "Your inventory is locked. Wait until it is loaded.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryInteract(InventoryInteractEvent event)
    {
        HumanEntity player = event.getWhoClicked();
        if(locked.contains(player.getName()))
        {
            player.sendMessage(Prefixes.INFO +
                "Your inventory is locked. Wait until it is loaded.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDropItemEvent(PlayerDropItemEvent event)
    {
        Player player = event.getPlayer();
        if(locked.contains(player.getName()))
        {
            player.sendMessage(Prefixes.INFO +
                "Your inventory is locked. Please wait until it is loaded.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerPickupItemEvent(PlayerPickupItemEvent event)
    {
        Player player = event.getPlayer();
        if(locked.contains(player.getName()))
        {
            player.sendMessage(Prefixes.INFO +
                "Your inventory is locked. Please wait until it is loaded.");
            event.setCancelled(true);
        }
    }
}

package com.gmail.tracebachi.DeltaEssentials.Events;

import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 7/16/16.
 */
public class PlayerLoadRequestEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();
    private final Player player;

    public PlayerLoadRequestEvent(Player player)
    {
        this.player = Preconditions.checkNotNull(player);
    }

    public Player getPlayer()
    {
        return player;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}

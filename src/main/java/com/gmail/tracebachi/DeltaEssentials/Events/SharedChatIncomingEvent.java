package com.gmail.tracebachi.DeltaEssentials.Events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 7/16/16.
 */
public class SharedChatIncomingEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private final String channel;
    private final String permission;
    private final String message;
    private boolean cancelled;

    public SharedChatIncomingEvent(String channel, String permission, String message)
    {
        this.channel = channel;
        this.permission = permission;
        this.message = message;
    }

    public String getChannel()
    {
        return channel;
    }

    public String getPermission()
    {
        return permission;
    }

    public String getMessage()
    {
        return message;
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

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }
}

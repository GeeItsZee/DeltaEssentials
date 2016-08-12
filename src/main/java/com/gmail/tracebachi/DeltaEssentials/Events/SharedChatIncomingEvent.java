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
package com.gmail.tracebachi.DeltaEssentials.Events;

import com.google.common.base.Preconditions;
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
    private final String extraInfo;
    private boolean cancelled;

    public SharedChatIncomingEvent(String channel, String permission, String message)
    {
        this(channel, permission, message, "");
    }

    public SharedChatIncomingEvent(String channel, String permission, String message,
                                   String extraInfo)
    {
        this.channel = Preconditions.checkNotNull(channel, "Channel was null.");
        this.permission = Preconditions.checkNotNull(permission, "Permission was null.");
        this.message = Preconditions.checkNotNull(message, "Message was null.");
        this.extraInfo = Preconditions.checkNotNull(extraInfo, "ExtraInfo was null.");
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

    public String getExtraInfo()
    {
        return extraInfo;
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

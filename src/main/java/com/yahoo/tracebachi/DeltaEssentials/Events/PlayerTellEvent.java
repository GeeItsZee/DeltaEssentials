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
package com.yahoo.tracebachi.DeltaEssentials.Events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class PlayerTellEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    private final String sender;
    private final String receiver;
    private String message;
    private boolean cancelled;

    public PlayerTellEvent(String sender, String receiver, String message)
    {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }

    public String getSender()
    {
        return sender;
    }

    public String getReceiver()
    {
        return receiver;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public HandlerList getHandlers()
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

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}

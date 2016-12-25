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

import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Callback;
import com.gmail.tracebachi.DeltaRedis.Spigot.Events.DelayedHandingEvent;
import com.google.common.base.Preconditions;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.Set;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/24/16.
 */
public class PlayerTellReceiveEvent extends DelayedHandingEvent<PlayerTellReceiveEvent> implements Cancellable
{
    private final String senderName;
    private final String receiverName;
    private final Set<String> senderPermissions;
    private String message;
    private boolean cancelled = false;

    public PlayerTellReceiveEvent(String senderName, String receiverName, String message,
                                  Set<String> senderPermissions,
                                  Callback<PlayerTellReceiveEvent> callback)
    {
        super(callback);

        Preconditions.checkNotNull(senderName, "senderName");
        Preconditions.checkNotNull(receiverName, "receiverName");
        Preconditions.checkNotNull(message, "message");
        Preconditions.checkNotNull(senderPermissions, "senderPermissions");

        this.senderName = senderName;
        this.receiverName = receiverName;
        this.message = message;
        this.senderPermissions = Collections.unmodifiableSet(senderPermissions);
    }

    /**
     * @return Name of the sender
     */
    public String getSenderName()
    {
        return senderName;
    }

    /**
     * @return Name of the receiver
     */
    public String getReceiverName()
    {
        return receiverName;
    }

    /**
     * @return Message to send
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Checks if the sender was marked as having the permission when
     * they sent the message
     *
     * @param permission Permission to check
     * @return True if the sender was marked as having the permission
     */
    public boolean hasSenderPermission(String permission)
    {
        return senderPermissions.contains(permission);
    }

    /**
     * @param message Modified non-null message to send
     */
    public void setMessage(String message)
    {
        Preconditions.checkNotNull(message, "message");
        this.message = message;
    }

    /** Used by Bukkit and Spigot **/
    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    /** Used by Bukkit and Spigot **/
    @Override
    public void setCancelled(boolean val)
    {
        cancelled = val;
    }

    /** Used by Bukkit and Spigot **/
    private static final HandlerList handlers = new HandlerList();

    /** Used by Bukkit and Spigot **/
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    /** Used by Bukkit and Spigot **/
    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}

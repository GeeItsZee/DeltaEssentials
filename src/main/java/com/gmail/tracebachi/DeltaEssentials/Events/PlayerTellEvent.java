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

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class PlayerTellEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final String senderName;
    private final CommandSender sender;
    private final String receiverName;
    private final CommandSender receiver;
    private String message;

    public PlayerTellEvent(String senderName, CommandSender sender,
        String receiverName, CommandSender receiver, String message)
    {
        this.senderName = senderName;
        this.sender = sender;
        this.receiverName = receiverName;
        this.receiver = receiver;
        this.message = message;
    }

    public String getSenderName()
    {
        return senderName;
    }

    public CommandSender getSender()
    {
        return sender;
    }

    public String getReceiverName()
    {
        return receiverName;
    }

    public CommandSender getReceiver()
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

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}

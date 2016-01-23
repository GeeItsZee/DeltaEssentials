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

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Trace Bachi (BigBossZee) on 6/16/2015.
 */
public class PlayerServerSwitchEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String destinationServer;
    private boolean cancelled = false;

    public PlayerServerSwitchEvent(Player player, String destinationServer)
    {
        this.player = player;
        this.destinationServer = destinationServer;
    }

    public Player getPlayer()
    {
        return player;
    }

    public String getDestinationServer()
    {
        return destinationServer;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean val)
    {
        cancelled = val;
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

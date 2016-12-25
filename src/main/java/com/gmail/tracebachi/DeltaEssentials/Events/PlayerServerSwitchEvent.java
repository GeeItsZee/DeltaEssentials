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
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Trace Bachi (BigBossZee) on 6/16/2015.
 */
public class PlayerServerSwitchEvent extends Event implements Cancellable
{
    private final Player player;
    private final String destServer;
    private boolean cancelled = false;

    public PlayerServerSwitchEvent(Player player, String destServer)
    {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(destServer, "destServer");

        this.player = player;
        this.destServer = destServer;
    }

    /**
     * @return Player that is attempting to switch servers
     */
    public Player getPlayer()
    {
        return player;
    }

    /**
     * @return Name of the server that the player is attempting to switch to
     */
    public String getDestServer()
    {
        return destServer;
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

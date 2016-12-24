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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 7/16/16.
 */
public class PlayerLoadRequestEvent extends Event
{
    private final Player player;

    public PlayerLoadRequestEvent(Player player)
    {
        this.player = Preconditions.checkNotNull(player);
    }

    /**
     * @return Player to try and load
     */
    public Player getPlayer()
    {
        return player;
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

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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerPostLoadEvent extends Event
{
    private final Player player;
    private final ConfigurationSection metaData;
    private final boolean firstJoin;

    public PlayerPostLoadEvent(Player player, ConfigurationSection metaData)
    {
        this(player, metaData, false);
    }

    public PlayerPostLoadEvent(Player player, ConfigurationSection metaData, boolean firstJoin)
    {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(metaData, "metaData");

        this.player = player;
        this.metaData = metaData;
        this.firstJoin = firstJoin;
    }

    /**
     * @return Player that was loaded
     */
    public Player getPlayer()
    {
        return player;
    }

    /**
     * @return Metadata stored in the data file for the loaded player
     */
    public ConfigurationSection getMetaData()
    {
        return metaData;
    }

    /**
     * @return True if the player's data file did not exist or false
     */
    public boolean isFirstJoin()
    {
        return firstJoin;
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

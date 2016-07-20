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
package com.gmail.tracebachi.DeltaEssentials.Storage;

import com.google.common.base.Preconditions;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerEntry
{
    private final String name;
    private PlayerStats playerStats;
    private DeltaEssPlayerData deltaEssPlayerData;

    public PlayerEntry(String name)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");
        this.name = name.toLowerCase();
    }

    public String getName()
    {
        return name;
    }

    public PlayerStats getPlayerStats()
    {
        return playerStats;
    }

    public void setPlayerStats(PlayerStats playerStats)
    {
        this.playerStats = playerStats;
    }

    public DeltaEssPlayerData getDeltaEssPlayerData()
    {
        return deltaEssPlayerData;
    }

    public void setDeltaEssPlayerData(DeltaEssPlayerData deltaEssPlayerData)
    {
        this.deltaEssPlayerData = deltaEssPlayerData;
    }
}

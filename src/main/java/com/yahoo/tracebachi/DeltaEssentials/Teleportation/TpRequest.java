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
package com.yahoo.tracebachi.DeltaEssentials.Teleportation;

import com.yahoo.tracebachi.DeltaRedis.Shared.Cache.Cacheable;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/5/15.
 */
public class TpRequest implements Cacheable
{
    private final String sender;
    private final String server;
    private final long timeCreatedAt;

    public TpRequest(String sender, String server)
    {
        this(sender, server, System.currentTimeMillis());
    }

    public TpRequest(String sender, String server, long timeCreatedAt)
    {
        this.sender = sender;
        this.server = server;
        this.timeCreatedAt = timeCreatedAt;
    }

    public String getSender()
    {
        return sender;
    }

    public String getServer()
    {
        return server;
    }

    @Override
    public long getTimeCreatedAt()
    {
        return timeCreatedAt;
    }
}

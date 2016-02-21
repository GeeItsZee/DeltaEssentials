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

import com.gmail.tracebachi.DeltaRedis.Shared.Cache.Cacheable;
import com.google.common.base.Preconditions;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/5/15.
 */
public class TeleportRequest implements Cacheable
{
    private final String sender;
    private final String destServer;
    private final long timeCreatedAt;
    private final boolean tpOtherToSelf;

    public TeleportRequest(String sender, String destServer, boolean tpOtherToSelf)
    {
        this(sender, destServer, tpOtherToSelf, System.currentTimeMillis());
    }

    public TeleportRequest(String sender, String destServer, boolean tpOtherToSelf, long timeCreatedAt)
    {
        Preconditions.checkNotNull(sender);
        Preconditions.checkNotNull(destServer);

        this.sender = sender;
        this.destServer = destServer;
        this.tpOtherToSelf = tpOtherToSelf;
        this.timeCreatedAt = timeCreatedAt;
    }

    public String getSender()
    {
        return sender;
    }

    public String getDestServer()
    {
        return destServer;
    }

    public boolean isTpOtherToSelf()
    {
        return tpOtherToSelf;
    }

    @Override
    public long getTimeCreatedAt()
    {
        return timeCreatedAt;
    }
}

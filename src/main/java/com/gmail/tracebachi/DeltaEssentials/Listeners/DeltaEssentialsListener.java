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
package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/22/16.
 */
public class DeltaEssentialsListener implements Listener, Registerable, Shutdownable
{
    protected static final Pattern DELTA_PATTERN = Pattern.compile("/\\\\");

    protected DeltaEssentials plugin;

    public DeltaEssentialsListener(DeltaEssentials plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister()
    {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void shutdown()
    {
        this.plugin = null;
    }
}

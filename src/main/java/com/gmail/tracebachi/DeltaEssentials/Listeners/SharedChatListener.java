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
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsChannels;
import com.gmail.tracebachi.DeltaEssentials.Events.SharedChatIncomingEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.SharedChatOutgoingEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import static com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent.DELTA_PATTERN;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class SharedChatListener extends DeltaEssentialsListener
{
    private DeltaRedisApi deltaRedisApi;

    public SharedChatListener(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super(plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void shutdown()
    {
        this.deltaRedisApi = null;
        super.shutdown();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onSharedChatOutgoing(SharedChatOutgoingEvent event)
    {
        String channel = event.getChannel();
        String permission = event.getPermission();
        String message = event.getMessage();

        deltaRedisApi.publish(Servers.SPIGOT, DeltaEssentialsChannels.SHARED_CHAT,
            channel, permission, message);
    }

    @EventHandler
    public void onSharedChatIncoming(DeltaRedisMessageEvent event)
    {
        if(!event.getChannel().equals(DeltaEssentialsChannels.SHARED_CHAT)) return;

        String[] splitMessage = DELTA_PATTERN.split(event.getMessage(), 3);
        String channelName = splitMessage[0];
        String permission = splitMessage[1];
        String message = splitMessage[2];
        SharedChatIncomingEvent chatEvent = new SharedChatIncomingEvent(channelName, permission, message);

        Bukkit.getPluginManager().callEvent(chatEvent);

        // If the message is not handled by anyone else, return
        if(chatEvent.isCancelled()) return;

        plugin.getServer().broadcast(message, permission);
    }
}

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
import com.gmail.tracebachi.DeltaRedis.Shared.SplitPatterns;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class SharedChatListener extends DeltaEssentialsListener
{
    public SharedChatListener(DeltaEssentials plugin)
    {
        super(plugin);
    }

    @Override
    public void shutdown()
    {
        super.shutdown();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSharedChatOutgoing(SharedChatOutgoingEvent event)
    {
        String channel = event.getChannel();
        String permission = event.getPermission();
        String message = event.getMessage();
        String extraInfo = event.getExtraInfo();

        DeltaRedisApi.instance().publish(
            Servers.SPIGOT,
            DeltaEssentialsChannels.CHAT,
            channel,
            permission,
            message,
            extraInfo);
    }

    @EventHandler
    public void onSharedChatIncoming(DeltaRedisMessageEvent event)
    {
        if(!event.getChannel().equals(DeltaEssentialsChannels.CHAT)) { return; }

        if(!event.isSendingServerSelf()) { return; }

        String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 4);
        String channelName = splitMessage[0];
        String permission = splitMessage[1];
        String message = splitMessage[2];
        String extraInfo = splitMessage[3];
        SharedChatIncomingEvent chatEvent = new SharedChatIncomingEvent(
            channelName,
            permission,
            message,
            extraInfo);

        Bukkit.getPluginManager().callEvent(chatEvent);
    }
}

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

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.ChatCompleteEvent;
import com.dthielke.herochat.Herochat;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsChannels;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.event.EventHandler;

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

    // TODO Get rid of HeroChat dependency. Make a plugin and an event that plugin can fire to trigger this
    @EventHandler
    public void onChatComplete(ChatCompleteEvent event)
    {
        String channel = event.getChannel().getName();
        Boolean useHeroChat = Settings.useHeroChatForSharedChatChannel(channel);

        if(!useHeroChat) return;

        String message =  event.getMsg();

        deltaRedisApi.publish(Servers.SPIGOT, DeltaEssentialsChannels.SHARED_CHAT,
            channel, useHeroChat.toString(), message);
    }

    @EventHandler
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.SHARED_CHAT))
        {
            String[] splitMessage = DELTA_PATTERN.split(event.getMessage(), 3);
            String channelName = splitMessage[0];
            String permission = splitMessage[1];
            String message = splitMessage[2];
            Channel channel = Herochat.getChannelManager().getChannel(channelName);

            if(channel == null)
            {
                plugin.severe("HeroChat channel (" + channelName + ") not found!");
                return;
            }

            if(permission.equalsIgnoreCase("false"))
            {
                channel.sendRawMessage(message);
                return;
            }

            // TODO SharedChatEvent
            plugin.getServer().broadcast(message, permission);
        }
    }
}

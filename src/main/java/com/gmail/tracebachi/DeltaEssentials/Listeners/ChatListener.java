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
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTellEvent;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssentialsPlayer;
import com.gmail.tracebachi.DeltaEssentials.Utils.MessageUtils;
import com.gmail.tracebachi.DeltaRedis.Shared.Redis.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.nio.charset.StandardCharsets;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 11/29/15.
 */
public class ChatListener extends DeltaEssentialsListener
{
    private DeltaRedisApi deltaRedisApi;

    public ChatListener(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
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

    @EventHandler
    public void onChatComplete(ChatCompleteEvent event)
    {
        Settings settings = plugin.getSettings();
        String channel = event.getChannel().getName();

        if(settings.isChatChannelShared(channel))
        {
            String message =  event.getMsg();
            deltaRedisApi.publish(Servers.SPIGOT, DeltaEssentialsChannels.SHARED_CHAT,
                channel + "/\\" + message);
        }
    }

    @EventHandler
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaEssentialsChannels.SHARED_CHAT))
        {
            String[] splitMessage = DELTA_PATTERN.split(event.getMessage(), 2);
            String channelName = splitMessage[0];
            String message = splitMessage[1];
            Channel channel = Herochat.getChannelManager().getChannel(channelName);

            if(channel != null)
            {
                channel.sendRawMessage(message);
            }
            else
            {
                plugin.severe("HeroChat channel (" + channelName + ") not found!");
            }
        }
        else if(event.getChannel().equals(DeltaEssentialsChannels.TELL))
        {
            onTellMessage(event);
        }
    }

    private void onTellMessage(DeltaRedisMessageEvent event)
    {
        byte[] bytes = event.getMessage().getBytes(StandardCharsets.UTF_16);
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String sender = in.readUTF();
        String receiver = in.readUTF();
        String message = in.readUTF();
        Player player = Bukkit.getPlayer(receiver);

        if(player != null && player.isOnline())
        {
            PlayerTellEvent tellEvent = plugin.sendMessageFromPlayer(sender, receiver, message);

            if(!tellEvent.isCancelled())
            {
                // In case the message was modified, update it
                message = tellEvent.getMessage();

                // Send message
                player.sendMessage(MessageUtils.formatForReceiver(sender, message));

                // Update last reply target
                DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(receiver);
                if(dePlayer != null)
                {
                    dePlayer.setLastReplyTarget(sender);
                }
            }
            else if(tellEvent.getCancelReason() != null)
            {
                deltaRedisApi.sendMessageToPlayer(sender, tellEvent.getCancelReason());
            }
        }
        else if(!sender.equalsIgnoreCase("console"))
        {
            deltaRedisApi.sendMessageToPlayer(sender,
                Prefixes.FAILURE + "Player not found.");
        }
    }
}

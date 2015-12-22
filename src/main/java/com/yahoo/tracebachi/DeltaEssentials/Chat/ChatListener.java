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
package com.yahoo.tracebachi.DeltaEssentials.Chat;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.ChatCompleteEvent;
import com.dthielke.herochat.Herochat;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTellEvent;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public class ChatListener implements Listener
{
    private static final Pattern pattern = Pattern.compile("/\\\\");

    private HashMap<String, String> replyMap;
    private DeltaRedisApi deltaRedisApi;
    private DeltaChat deltaChat;

    public ChatListener(HashMap<String, String> replyMap, DeltaRedisApi deltaRedisApi, DeltaChat deltaChat)
    {
        this.replyMap = replyMap;
        this.deltaRedisApi = deltaRedisApi;
        this.deltaChat = deltaChat;
    }

    public void shutdown()
    {
        replyMap = null;
        deltaRedisApi = null;
        deltaChat = null;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        replyMap.remove(event.getPlayer().getName().toLowerCase());
    }

    @EventHandler
    public void onChatComplete(ChatCompleteEvent event)
    {
        String channel = event.getChannel().getName();

        if(!deltaChat.getIgnoredHeroChatChannels().contains(channel))
        {
            String message =  event.getMsg();
            deltaRedisApi.publish(Channels.SPIGOT, DeltaChat.HERO_CHAT_CHANNEL,
                channel + "/\\" + message);
        }
    }

    @EventHandler
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaChat.HERO_CHAT_CHANNEL))
        {
            String[] splitMessage = pattern.split(event.getMessage(), 2);
            String channelName = splitMessage[0];
            String message = splitMessage[1];
            Channel channel = Herochat.getChannelManager().getChannel(channelName);

            if(channel != null)
            {
                channel.sendRawMessage(message);
            }
            else
            {
                deltaChat.severe("Chat channel (" + channelName + ") not found!");
            }
        }
        else if(event.getChannel().equals(DeltaChat.TELL_CHANNEL))
        {
            onTellMessage(event);
        }
    }

    private void onTellMessage(DeltaRedisMessageEvent event)
    {
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getMessage().getBytes(StandardCharsets.UTF_8));
        String sender = in.readUTF();
        String receiver = in.readUTF();
        String message = in.readUTF();
        boolean allowColors = in.readBoolean();
        Player player = Bukkit.getPlayer(receiver);

        if(player != null && player.isOnline())
        {
            receiver = player.getName();

            PlayerTellEvent tellEvent = deltaChat.tellWithEvent(sender, receiver, message);
            if(tellEvent.isCancelled()) { return; }

            message = tellEvent.getMessage();
            player.sendMessage(MessageUtils.format(sender, receiver, message, allowColors));
            replyMap.put(receiver, sender);
        }
        else if(!sender.equalsIgnoreCase("console"))
        {
            deltaRedisApi.sendMessageToPlayer(sender, Prefixes.FAILURE + "Player not found.");
        }
    }
}

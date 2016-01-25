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
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.Map;

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
            deltaRedisApi.publish(Servers.SPIGOT,
                DeltaEssentialsChannels.SHARED_CHAT,
                channel, message);
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

    public boolean sendMessageFromPlayer(String senderName, CommandSender sender,
        String receiverName, CommandSender receiver, String message)
    {
        Settings settings = plugin.getSettings();
        PlayerTellEvent event = new PlayerTellEvent(senderName, sender,
            receiverName, receiver, message);

        Bukkit.getPluginManager().callEvent(event);

        if(!event.isCancelled())
        {
            String logFormat = settings.format("TellLog", senderName, receiverName, message);
            Bukkit.getLogger().info(logFormat);

            String spyFormat = settings.format("TellSpy", senderName, receiverName, message);
            sendToAllSocialSpies(spyFormat);

            if(sender != null)
            {
                String senderFormat = settings.format("TellSender", receiverName, message);
                sender.sendMessage(senderFormat);

                DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(senderName);
                dePlayer.setLastReplyTarget(receiverName);
            }

            if(receiver != null)
            {
                String receiverFormat = settings.format("TellReceiver", senderName, message);
                receiver.sendMessage(receiverFormat);

                DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(receiverName);
                dePlayer.setLastReplyTarget(senderName);
            }

            return true;
        }

        return false;
    }

    private void sendToAllSocialSpies(String spyFormat)
    {
        for(Map.Entry<String, DeltaEssentialsPlayer> entry : plugin.getPlayerMap().entrySet())
        {
            if(entry.getValue().isSocialSpyEnabled())
            {
                Player player = Bukkit.getPlayer(entry.getKey());

                if(player != null)
                {
                    player.sendMessage(spyFormat);
                }
            }
        }
    }

    private void onTellMessage(DeltaRedisMessageEvent event)
    {
        Settings settings = plugin.getSettings();
        String[] split = DELTA_PATTERN.split(event.getMessage(), 3);
        String sender = split[0];
        String receiver = split[1];
        String message = split[2];
        Player player = Bukkit.getPlayer(receiver);

        if(player != null)
        {
            sendMessageFromPlayer(sender, null, receiver, player, message);
        }
        else if(!sender.equalsIgnoreCase("console"))
        {
            String playerNotOnline = settings.format("PlayerNotOnline", receiver);
            deltaRedisApi.sendMessageToPlayer(sender, playerNotOnline);
        }
    }
}

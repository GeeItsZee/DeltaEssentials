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
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerTellEvent;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Map;

import static com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent.DELTA_PATTERN;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 3/21/16.
 */
public class TellChatListener extends DeltaEssentialsListener
{
    private DeltaRedisApi deltaRedisApi;

    public TellChatListener(DeltaRedisApi deltaRedisApi, DeltaEssentials plugin)
    {
        super(plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void shutdown()
    {
        deltaRedisApi = null;
        super.shutdown();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();

        if(channel.equals(DeltaEssentialsChannels.TELL))
        {
            String[] split = DELTA_PATTERN.split(event.getMessage(), 3);
            String senderName = split[0];
            String receiverName = split[1];
            String message = split[2];
            Player receiver = Bukkit.getPlayer(receiverName);

            if(receiver == null)
            {
                deltaRedisApi.sendMessageToPlayer(senderName,
                    Settings.format("PlayerOffline", receiverName));
                return;
            }

            DeltaEssPlayerData playerData = plugin.getPlayerMap().get(receiverName);

            if(playerData != null && playerData.isVanishEnabled())
            {
                deltaRedisApi.sendMessageToPlayer(senderName,
                    Settings.format("PlayerOffline", receiverName));
                return;
            }

            sendMessage(
                senderName, null,
                receiverName, receiver,
                message, false);
        }
        else if(channel.equals(DeltaEssentialsChannels.TELL_SPY))
        {
            String[] split = DELTA_PATTERN.split(event.getMessage(), 3);
            String senderName = split[0];
            String receiverName = split[1];
            String message = split[2];

            sendToSpies(senderName, receiverName, message);
        }
    }

    public boolean sendMessage(String senderName, CommandSender sender,
        String receiverName, CommandSender receiver, String message, boolean sendToSpies)
    {
        Map<String, DeltaEssPlayerData> playerMap = plugin.getPlayerMap();
        PlayerTellEvent event = new PlayerTellEvent(senderName, sender,
            receiverName, receiver, message);

        Bukkit.getPluginManager().callEvent(event);

        if(event.isCancelled()) return false;

        if(sender != null)
        {
            String senderFormat = Settings.format("TellSender", receiverName, message);
            sender.sendMessage(senderFormat);

            DeltaEssPlayerData dePlayer = playerMap.get(senderName);

            if(dePlayer != null)
            {
                dePlayer.setReplyTo(receiverName);
            }
        }

        if(receiver != null)
        {
            String receiverFormat = Settings.format("TellReceiver", senderName, message);
            receiver.sendMessage(receiverFormat);

            DeltaEssPlayerData dePlayer = playerMap.get(receiverName);

            if(dePlayer != null)
            {
                dePlayer.setReplyTo(senderName);
            }
        }

        String logFormat = Settings.format("TellLog", senderName, receiverName, message);
        Bukkit.getLogger().info(logFormat);

        if(sendToSpies)
        {
            sendToSpies(senderName, receiverName, message);

            deltaRedisApi.publish(Servers.SPIGOT, DeltaEssentialsChannels.TELL_SPY,
                senderName, receiverName, message);
        }

        return true;
    }

    private void sendToSpies(String senderName, String receiverName, String message)
    {
        String spyFormat = Settings.format("TellSpy", senderName, receiverName, message);

        for(Map.Entry<String, DeltaEssPlayerData> entry : plugin.getPlayerMap().entrySet())
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
}

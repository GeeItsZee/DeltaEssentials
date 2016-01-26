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
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/22/16.
 */
public class MessageListener extends DeltaEssentialsListener
{
    public MessageListener(DeltaEssentials plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onRedisMessageEvent(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();

        if(channel.equals(DeltaEssentialsChannels.MOVE))
        {
            String[] split = DELTA_PATTERN.split(event.getMessage(), 3);
            String sender = split[0];
            String nameToMove = split[1];
            String destination = split[2];

            Player toMove = Bukkit.getPlayer(nameToMove);

            if(toMove != null)
            {
                plugin.sendToServer(toMove, destination);
                plugin.info(sender + " moved " + nameToMove + " to " + destination);
            }
        }
        else if(channel.equals(DeltaEssentialsChannels.KICK))
        {
            String[] split = DELTA_PATTERN.split(event.getMessage(), 3);
            String sender = split[0];
            String target = split[1];
            String reason = split[2];

            Settings settings = plugin.getSettings();
            Player toKick = Bukkit.getPlayer(target);

            if(toKick != null)
            {
                String kickPlayer = settings.format("KickPlayer", sender, reason);
                toKick.kickPlayer(kickPlayer);
            }

            String kickAnnounce = settings.format("KickAnnounce", sender, target, reason);

            for(Player onlinePlayer : Bukkit.getOnlinePlayers())
            {
                onlinePlayer.sendMessage(kickAnnounce);
            }
        }
        else if(channel.equals(DeltaEssentialsChannels.JAIL))
        {
            String[] split = DELTA_PATTERN.split(event.getMessage(), 2);
            String sender = split[0];
            String command = split[1];

            plugin.info(sender + " ran /essentials:jail " + command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "essentials:jail " + command);
        }
    }
}

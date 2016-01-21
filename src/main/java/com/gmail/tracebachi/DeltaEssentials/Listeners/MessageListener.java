package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsChannels;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            if(toMove != null && toMove.isOnline())
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

            String kickMessage = plugin.getSettings().getKickMessage(sender, reason);
            Player toKick = Bukkit.getPlayer(target);

            if(toKick != null && toKick.isOnline())
            {
                toKick.kickPlayer(kickMessage);
            }

            announceKick(sender, target, reason);
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

    private void announceKick(String kicker, String playerKicked, String reason)
    {
        String announcement =
            ChatColor.GOLD + kicker + ChatColor.WHITE + " kicked " +
                ChatColor.GOLD + playerKicked + ChatColor.WHITE + " for " +
                ChatColor.GOLD + reason;

        for(Player onlinePlayer : Bukkit.getOnlinePlayers())
        {
            onlinePlayer.sendMessage(announcement);
        }
    }
}

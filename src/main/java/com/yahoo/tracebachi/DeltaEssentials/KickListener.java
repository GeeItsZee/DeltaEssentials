package com.yahoo.tracebachi.DeltaEssentials;

import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/12/15.
 */
public class KickListener implements Listener
{
    private static final Pattern pattern = Pattern.compile("/\\\\");

    @EventHandler
    public void onRedisMessageEvent(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(KickCommand.KICK_CHANNEL))
        {
            String[] splitMessage = pattern.split(event.getMessage(), 3);
            String target = splitMessage[0];
            String sender = splitMessage[1];
            String kickMesage = splitMessage[2];

            Player targetPlayer = Bukkit.getPlayer(target);
            if(targetPlayer != null && targetPlayer.isOnline())
            {
                targetPlayer.kickPlayer(kickMesage + "\n\n - " + ChatColor.GOLD + sender);

                for(Player onlinePlayer : Bukkit.getOnlinePlayers())
                {
                    onlinePlayer.sendMessage(
                        ChatColor.GOLD + sender +
                        ChatColor.WHITE + " kicked " +
                        ChatColor.GOLD + target +
                        ChatColor.WHITE + " for " +
                        ChatColor.GOLD + kickMesage);
                }
            }
        }
    }
}

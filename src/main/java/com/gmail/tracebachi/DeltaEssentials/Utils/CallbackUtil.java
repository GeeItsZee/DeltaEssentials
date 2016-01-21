package com.gmail.tracebachi.DeltaEssentials.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/13/15.
 */
public interface CallbackUtil
{
    static void sendMessage(String playerName, String message)
    {
        if(playerName.equalsIgnoreCase("console"))
        {
            Bukkit.getConsoleSender().sendMessage(message);
        }
        else
        {
            Player player = Bukkit.getPlayer(playerName);
            if(player != null && player.isOnline())
            {
                player.sendMessage(message);
            }
        }
    }
}

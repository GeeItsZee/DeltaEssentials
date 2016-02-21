package com.gmail.tracebachi.DeltaEssentials.Utils;

import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 2/21/16.
 */
public interface CommandMessageUtil
{
    static void onlyForConsole(CommandSender sender, String command)
    {
        sender.sendMessage(Prefixes.FAILURE + "Only console can use /" + command);
    }

    static void onlyForPlayers(CommandSender sender, String command)
    {
        sender.sendMessage(Prefixes.FAILURE + "Only players can use /" + command);
    }

    static void noPermission(CommandSender sender, String permission)
    {
        sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
            Prefixes.input(permission) + " permission.");
    }

    static void playerOffline(CommandSender sender, String playerName)
    {
        sender.sendMessage(Prefixes.FAILURE + Prefixes.input(playerName) +
            " is not online.");
    }

    static void playerOffline(String senderName, String playerName)
    {
        if(senderName.equalsIgnoreCase("console"))
        {
            Bukkit.getConsoleSender().sendMessage(Prefixes.FAILURE + Prefixes.input(playerName) +
                " is not online.");
        }
        else
        {
            Player player = Bukkit.getPlayer(senderName);

            if(player != null)
            {
                player.sendMessage(Prefixes.FAILURE + Prefixes.input(playerName) +
                    " is not online.");
            }
        }
    }
}

package com.yahoo.tracebachi.DeltaEssentials;

import com.yahoo.tracebachi.DeltaEssentials.Commands.JailCommand;
import com.yahoo.tracebachi.DeltaEssentials.Commands.KickCommand;
import com.yahoo.tracebachi.DeltaEssentials.Commands.MoveToCommand;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/12/15.
 */
public class GeneralListener implements Listener
{
    private static final Pattern pattern = Pattern.compile("/\\\\");

    private DeltaEssentialsPlugin plugin;

    public GeneralListener(DeltaEssentialsPlugin plugin)
    {
        this.plugin = plugin;
    }

    public void shutdown()
    {
        this.plugin = null;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerLoginEvent(PlayerLoginEvent event)
    {
        if(plugin.isStopJoinEnabled())
        {
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(Prefixes.FAILURE + "This server is currently not allowing players to join.");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onRedisMessageEvent(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();

        if(channel.equals(MoveToCommand.MOVE_CHANNEL))
        {
            String[] splitMessage = pattern.split(event.getMessage(), 3);
            String sender = splitMessage[0];
            String nameToMove = splitMessage[1];
            String destination = splitMessage[2];

            Player playerToMove = Bukkit.getPlayer(nameToMove);
            if(playerToMove != null && playerToMove.isOnline())
            {
                plugin.sendToServer(playerToMove, destination);
                plugin.info(sender + " moved " + nameToMove + " to " + destination);
            }
        }
        else if(channel.equals(KickCommand.KICK_CHANNEL))
        {
            String[] splitMessage = pattern.split(event.getMessage(), 3);
            String sender = splitMessage[0];
            String target = splitMessage[1];
            String reason = splitMessage[2];

            Player targetPlayer = Bukkit.getPlayer(target);
            if(targetPlayer != null && targetPlayer.isOnline())
            {
                targetPlayer.kickPlayer(reason + "\n\n by " + ChatColor.GOLD + sender);
                announceKick(sender, targetPlayer.getName(), reason);
            }
        }
        else if(channel.equals(JailCommand.JAIL_CHANNEL))
        {
            String[] splitMessage = pattern.split(event.getMessage(), 2);
            String sender = splitMessage[0];
            String command = splitMessage[1];

            plugin.info(sender + " ran /" + command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
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

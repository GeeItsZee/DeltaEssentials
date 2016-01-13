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

import com.yahoo.tracebachi.DeltaEssentials.Chat.Commands.ReplyCommand;
import com.yahoo.tracebachi.DeltaEssentials.Chat.Commands.SocialSpyCommand;
import com.yahoo.tracebachi.DeltaEssentials.Chat.Commands.TellCommand;
import com.yahoo.tracebachi.DeltaEssentials.DeltaEssentialsPlugin;
import com.yahoo.tracebachi.DeltaEssentials.Events.PlayerTellEvent;
import com.yahoo.tracebachi.DeltaRedis.Shared.Interfaces.LoggablePlugin;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/5/15.
 */
public class DeltaChat implements LoggablePlugin
{
    private DeltaEssentialsPlugin plugin;

    private HashMap<String, String> replyMap;
    private HashSet<String> socialSpyListeners;
    private Set<String> sharedChatChannels;

    private TellCommand tellCommand;
    private ReplyCommand replyCommand;
    private SocialSpyCommand socialSpyCommand;
    private ChatListener chatListener;

    public DeltaChat(DeltaRedisApi deltaRedisApi, DeltaEssentialsPlugin plugin)
    {
        this.plugin = plugin;

        this.replyMap = new HashMap<>();
        this.socialSpyListeners = new HashSet<>();
        this.sharedChatChannels = Collections.unmodifiableSet(new HashSet<>(
            plugin.getConfig().getStringList("SharedChatChannels")));

        this.tellCommand = new TellCommand(replyMap, deltaRedisApi, this);
        this.replyCommand = new ReplyCommand(replyMap, deltaRedisApi, this);
        this.socialSpyCommand = new SocialSpyCommand(this);

        plugin.getCommand("tell").setExecutor(tellCommand);
        plugin.getCommand("tell").setTabCompleter(tellCommand);
        plugin.getCommand("reply").setExecutor(replyCommand);
        plugin.getCommand("socialspy").setExecutor(socialSpyCommand);

        chatListener = new ChatListener(replyMap, deltaRedisApi, this);
        plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
    }

    public void shutdown()
    {
        if(chatListener != null)
        {
            chatListener.shutdown();
            chatListener = null;
        }

        if(socialSpyCommand != null)
        {
            plugin.getCommand("socialspy").setExecutor(null);
            socialSpyCommand.shutdown();
            socialSpyCommand = null;
        }

        if(tellCommand != null)
        {
            plugin.getCommand("tell").setExecutor(null);
            plugin.getCommand("tell").setTabCompleter(null);
            tellCommand.shutdown();
            tellCommand = null;
        }

        if(replyCommand != null)
        {
            plugin.getCommand("reply").setExecutor(null);
            replyCommand.shutdown();
            replyCommand = null;
        }

        if(socialSpyListeners != null)
        {
            socialSpyListeners.clear();
            socialSpyListeners = null;
        }

        if(replyMap != null)
        {
            replyMap.clear();
            replyMap = null;
        }

        sharedChatChannels = null;
        plugin = null;
    }

    public Set<String> getSharedChatChannels()
    {
        return sharedChatChannels;
    }

    public boolean isSocialSpy(String name)
    {
        return socialSpyListeners.contains(name);
    }

    public void addSocialSpy(String name)
    {
        socialSpyListeners.add(name);
    }

    public void removeSocialSpy(String name)
    {
        socialSpyListeners.remove(name);
    }

    public PlayerTellEvent tellWithEvent(String sender, String receiver, String message, boolean canUseColors)
    {
        if(canUseColors)
        {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        PlayerTellEvent event = new PlayerTellEvent(sender, receiver, message);
        Bukkit.getPluginManager().callEvent(event);

        if(!event.isCancelled())
        {
            // Log to the console
            String logFormat = MessageUtils.formatForLog(sender, receiver, message);
            Bukkit.getLogger().info(logFormat);

            // Send a copy of the message to the social spies
            for(String spyListener : socialSpyListeners)
            {
                Player player = Bukkit.getPlayer(spyListener);
                if(player != null && player.isOnline())
                {
                    player.sendMessage("[Spy]" + logFormat);
                }
            }
        }
        return event;
    }

    @Override
    public void info(String message)
    {
        plugin.getLogger().info(message);
    }

    @Override
    public void severe(String message)
    {
        plugin.getLogger().severe(message);
    }

    @Override
    public void debug(String message)
    {
        plugin.debug(message);
    }
}

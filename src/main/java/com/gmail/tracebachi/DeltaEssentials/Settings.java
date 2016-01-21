package com.gmail.tracebachi.DeltaEssentials;

import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashSet;
import org.bukkit.GameMode;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/20/16.
 */
public class Settings
{
    private static final Pattern SENDER_PATTERN = Pattern.compile("\\{SENDER\\}");
    private static final Pattern REASON_PATTERN = Pattern.compile("\\{REASON\\}");

    private boolean isOnLockdown;
    private boolean isDefaultGameModeForced;
    private String playerDataFolderPath;
    private String lockdownMessage;
    private String kickMessageFormat;
    private String jailServer;
    private GameMode defaultGameMode;
    private Set<GameMode> disabledGameModes;
    private Set<String> sharedChatChannels;
    private List<String> validJails;
    private CaseInsensitiveHashSet blockedServers;

    public Settings(DeltaEssentials plugin)
    {
        this.isOnLockdown = plugin.getConfig().getBoolean("StartWithLockdown");
        this.isDefaultGameModeForced = plugin.getConfig().getBoolean("ForceDefaultGameMode");
        this.playerDataFolderPath = plugin.getConfig().getString("PlayerDataFolder");
        this.lockdownMessage = plugin.getConfig().getString("LockdownMessage");
        this.kickMessageFormat = plugin.getConfig().getString("KickMessageFormat");
        this.jailServer = plugin.getConfig().getString("JailServer");
        this.defaultGameMode = getGameMode(plugin.getConfig().getString("DefaultGameMode"));
        this.validJails = plugin.getConfig().getStringList("ValidJails");
        this.disabledGameModes = new HashSet<>();
        this.sharedChatChannels = new HashSet<>();
        this.blockedServers = new CaseInsensitiveHashSet();

        for(String modeName : plugin.getConfig().getStringList("DisabledGameModes"))
        {
            try
            {
                this.disabledGameModes.add(GameMode.valueOf(modeName.toUpperCase()));
            }
            catch(IllegalArgumentException ignore) {}
        }

        for(String channel : plugin.getConfig().getStringList("SharedChatChannels"))
        {
            this.sharedChatChannels.add(channel);
        }

        for(String server : plugin.getConfig().getStringList("BlockedServers"))
        {
            this.blockedServers.add(server);
        }

        File file = new File(playerDataFolderPath);
        file.mkdirs();
    }

    public File getPlayerDataFileFor(String name)
    {
        String lowerCaseName = name.toLowerCase();
        File directory = new File(playerDataFolderPath +
            File.separator + lowerCaseName.charAt(0));

        directory.mkdirs();
        return new File(directory, lowerCaseName + ".yml");
    }

    public boolean isOnLockdown()
    {
        return isOnLockdown;
    }

    public void setOnLockdown(boolean onLockdown)
    {
        isOnLockdown = onLockdown;
    }

    public String getLockdownMessage()
    {
        return lockdownMessage;
    }

    public String getKickMessage(String kicker, String reason)
    {
        String senderReplaced = SENDER_PATTERN.matcher(kickMessageFormat).replaceAll(kicker);
        return REASON_PATTERN.matcher(senderReplaced).replaceAll(reason);
    }

    public String getJailServer()
    {
        return jailServer;
    }

    public GameMode getDefaultGameMode()
    {
        return defaultGameMode;
    }

    public boolean isDefaultGameModeForced()
    {
        return isDefaultGameModeForced;
    }

    public boolean isGameModeDisabled(GameMode gameMode)
    {
        return disabledGameModes.contains(gameMode);
    }

    public boolean isChatChannelShared(String channel)
    {
        return sharedChatChannels.contains(channel);
    }

    public boolean isServerBlocked(String serverName)
    {
        return blockedServers.contains(serverName.toLowerCase());
    }

    public boolean isValidJail(String jail)
    {
        return validJails.contains(jail);
    }

    private GameMode getGameMode(String source)
    {
        if(source == null) { return GameMode.SURVIVAL; }

        switch(source.toUpperCase())
        {
            default:
            case "SURVIVAL":
                return GameMode.SURVIVAL;
            case "CREATIVE":
                return GameMode.CREATIVE;
            case "ADVENTURE":
                return GameMode.ADVENTURE;
            case "SPECTATOR":
                return GameMode.SPECTATOR;
        }
    }
}

package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import org.bukkit.event.Listener;

import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/22/16.
 */
public class DeltaEssentialsListener implements Listener, Shutdownable
{
    protected static final Pattern DELTA_PATTERN = Pattern.compile("/\\\\");

    protected DeltaEssentials plugin;

    public DeltaEssentialsListener(DeltaEssentials plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void shutdown()
    {
        this.plugin = null;
    }

    public boolean register()
    {
        if(plugin != null)
        {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            return true;
        }
        return false;
    }
}

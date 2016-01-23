package com.gmail.tracebachi.DeltaEssentials.Commands;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/18/16.
 */
public abstract class DeltaEssentialsCommand implements TabExecutor, Shutdownable
{
    protected final String commandName;
    protected final String permission;
    protected DeltaEssentials plugin;

    public DeltaEssentialsCommand(String commandName, String permission, DeltaEssentials plugin)
    {
        this.commandName = commandName;
        this.permission = permission;
        this.plugin = plugin;
        this.plugin.getCommand(commandName).setExecutor(this);
    }

    public String getCommandName()
    {
        return commandName;
    }

    public String getPermission()
    {
        return permission;
    }

    public boolean register()
    {
        if(plugin != null)
        {
            plugin.getCommand(commandName).setExecutor(this);
            plugin.getCommand(commandName).setTabCompleter(this);
            return true;
        }
        return false;
    }

    public boolean unregister()
    {
        if(plugin != null)
        {
            plugin.getCommand(commandName).setExecutor(null);
            plugin.getCommand(commandName).setTabCompleter(null);
            return true;
        }
        return false;
    }

    @Override
    public void shutdown()
    {
        unregister();
        this.plugin = null;
    }

    public abstract void runCommand(CommandSender sender, Command command, String label, String[] args);

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args)
    {
        return null;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(permission != null && !sender.hasPermission(permission))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use this command.");
        }
        else
        {
            runCommand(sender, command, label, args);
        }
        return true;
    }
}

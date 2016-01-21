package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerLoadedEvent;
import com.gmail.tracebachi.DeltaEssentials.Events.PlayerPreSaveEvent;
import com.gmail.tracebachi.DeltaEssentials.Runnables.PlayerLoad;
import com.gmail.tracebachi.DeltaEssentials.Runnables.PlayerSave;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssentialsPlayer;
import com.gmail.tracebachi.DeltaEssentials.Storage.PlayerEntry;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedInventory;
import com.gmail.tracebachi.DeltaEssentials.Utils.xAuthUtil;
import com.gmail.tracebachi.DeltaRedis.Spigot.Prefixes;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.luricos.bukkit.xAuth.event.command.player.xAuthCommandLoginEvent;
import de.luricos.bukkit.xAuth.event.command.player.xAuthCommandRegisterEvent;
import de.luricos.bukkit.xAuth.event.player.xAuthPlayerJoinEvent;
import de.luricos.bukkit.xAuth.xAuthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public class PlayerDataIOListener extends DeltaEssentialsListener
{
    public PlayerDataIOListener(DeltaEssentials plugin)
    {
        super(plugin);
    }

    public void shutdown()
    {
        for(Player player : Bukkit.getOnlinePlayers())
        {
            try
            {
                savePlayerData(player, null);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                plugin.severe("Failed to save inventory on shutdown for " +
                    player.getName());
            }
        }
    }

    /**************************************************************************
     * Login Events
     *************************************************************************/

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerLoginEvent(PlayerLoginEvent event)
    {
        Settings settings = plugin.getSettings();
        if(settings.isOnLockdown())
        {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                settings.getLockdownMessage());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommandLoginEvent(xAuthCommandLoginEvent event)
    {
        if(event.getStatus() == xAuthPlayer.Status.AUTHENTICATED)
        {
            String name = event.getPlayerName();
            if(!plugin.getPlayerMap().containsKey(name))
            {
                Player player = Bukkit.getPlayer(name);
                if(player != null && player.isOnline())
                {
                    loadPlayerData(name);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoinEvent(xAuthPlayerJoinEvent event)
    {
        if(event.getStatus() == xAuthPlayer.Status.AUTHENTICATED)
        {
            String name = event.getPlayerName();
            if(!plugin.getPlayerMap().containsKey(name))
            {
                Player player = Bukkit.getPlayer(name);
                if(player != null && player.isOnline() )
                {
                    loadPlayerData(name);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerRegisterEvent(xAuthCommandRegisterEvent event)
    {
        String name = xAuthUtil.getPlayerNameFromRegisterEvent(event);
        if(name == null) { return; }

        if(event.getAction() == xAuthCommandRegisterEvent.Action.PLAYER_REGISTERED)
        {
            if(!plugin.getPlayerMap().containsKey(name))
            {
                Player player = Bukkit.getPlayer(name);
                if(player != null && player.isOnline())
                {
                    loadPlayerData(name);
                }
            }
        }
    }

    /**************************************************************************
     * Logout Event
     *************************************************************************/

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();

        if(plugin.getPlayerMap().containsKey(name))
        {
            savePlayerData(player, null);
            plugin.getInventoryLockListener().remove(name);
            plugin.getPlayerMap().remove(name);
        }
    }

    /**************************************************************************
     * GameModeChange Event
     *************************************************************************/

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();
        GameMode originalMode = player.getGameMode();
        GameMode newMode = event.getNewGameMode();
        Settings settings = plugin.getSettings();
        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(name);

        // Ignore game mode changes to the same mode
        if(originalMode == event.getNewGameMode()) { return; }

        // Ignore if DeltaEssPlayer does not exist
        if(dePlayer == null) { return; }

        // Check if the game mode is disabled
        if(settings.isGameModeDisabled(newMode) &&
            !player.hasPermission("DeltaEss.DisabledGameModeBypass." + newMode.name()))
        {
            player.sendMessage(Prefixes.FAILURE + Prefixes.input(newMode.name()) +
                "is disabled on this server.");
            event.setCancelled(true);
            return;
        }

        // Prevent game mode change during saves
        if(plugin.getInventoryLockListener().isLocked(name))
        {
            player.sendMessage(Prefixes.FAILURE + "Game mode changes while " +
                "inventory is locked are not allowed.");
            event.setCancelled(true);
            return;
        }

        // Prevent game mode changes unless there is no forced game mode or player is exempt
        if(settings.isDefaultGameModeForced() && newMode != settings.getDefaultGameMode() &&
            !player.hasPermission("DeltaEss.ForcedGameModeBypass"))
        {
            player.sendMessage(Prefixes.FAILURE + "Game mode is being forced. " +
                "You are not allowed to change it.");
            event.setCancelled(true);
            return;
        }

        // Ignore players that have the single inventory permission
        if(player.hasPermission("DeltaEss.SingleInventory")) { return; }

        // Save the inventory associated with the old game mode
        if(originalMode == GameMode.SURVIVAL)
        {
            dePlayer.setSurvival(new SavedInventory(player));
        }
        else if(originalMode == GameMode.CREATIVE)
        {
            dePlayer.setCreative(new SavedInventory(player));
        }

        // Apply the inventory associated with the new game mode
        if(newMode == GameMode.SURVIVAL)
        {
            SavedInventory savedSurvival = dePlayer.getSurvival();
            player.getInventory().setContents(savedSurvival.getContents());
            player.getInventory().setArmorContents(savedSurvival.getArmor());
            dePlayer.setSurvival(null);
        }
        else if(newMode == GameMode.CREATIVE)
        {
            SavedInventory savedCreative = dePlayer.getCreative();
            player.getInventory().setContents(savedCreative.getContents());
            player.getInventory().setArmorContents(savedCreative.getArmor());
            dePlayer.setCreative(null);
        }
        else
        {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
        }
    }

    /**************************************************************************
     * Player Loading / Saving Handing Methods
     *************************************************************************/

    public void onPlayerLoadSuccess(String name, PlayerEntry entry)
    {
        Player player = Bukkit.getPlayer(name);
        if(player != null && player.isOnline())
        {
            DeltaEssentialsPlayer dePlayer = new DeltaEssentialsPlayer();

            plugin.getInventoryLockListener().remove(name);
            plugin.getPlayerMap().put(name, dePlayer);
            applyPlayerEntry(player, entry, dePlayer);
        }
    }

    public void onPlayerNotFound(String name)
    {
        Player player = Bukkit.getPlayer(name);
        if(player != null && player.isOnline())
        {
            GameMode defaultGameMode = plugin.getSettings().getDefaultGameMode();
            DeltaEssentialsPlayer dePlayer = new DeltaEssentialsPlayer();

            plugin.getInventoryLockListener().remove(name);
            plugin.getPlayerMap().put(name, dePlayer);

            if(player.getGameMode() != defaultGameMode)
            {
                player.setGameMode(defaultGameMode);
            }

            PlayerLoadedEvent event = new PlayerLoadedEvent(player);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    public void onPlayerLoadException(String name)
    {
        Player player = Bukkit.getPlayer(name);
        if(player != null && player.isOnline())
        {
            player.sendMessage(Prefixes.FAILURE + "Failed to load inventory. " +
                "Refer to the console for more details.");
        }
    }

    public void onPlayerSaveSuccess(String name, String destServer)
    {
        Player player = Bukkit.getPlayer(name);
        if(player != null && player.isOnline())
        {
            if(destServer != null)
            {
                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                output.writeUTF("Connect");
                output.writeUTF(destServer);
                player.sendPluginMessage(plugin, "BungeeCord", output.toByteArray());
            }
            else
            {
                plugin.getInventoryLockListener().remove(name);
            }
        }
    }

    public void onPlayerSaveException(String name)
    {
        Player player = Bukkit.getPlayer(name);
        if(player != null && player.isOnline())
        {
            player.sendMessage(Prefixes.FAILURE + "Failed to save inventory. " +
                "Refer to the console for more details.");
        }
    }

    /**************************************************************************
     * Player Loading / Saving Scheduling Methods
     *************************************************************************/

    public void loadPlayerData(String name)
    {
        PlayerLoad runnable = new PlayerLoad(name, plugin);

        plugin.getInventoryLockListener().add(name);
        plugin.debug("Scheduling async player data load for {name:" + name + "}" );
        plugin.scheduleTaskAsync(runnable);
    }

    public void savePlayerData(Player player, String destServer)
    {
        String name = player.getName();
        DeltaEssentialsPlayer dePlayer = plugin.getPlayerMap().get(name);
        PlayerEntry entry = buildPlayerEntry(player, dePlayer);
        PlayerSave runnable = new PlayerSave(entry, destServer, plugin);

        plugin.getInventoryLockListener().add(name);
        plugin.debug("Scheduling async player data save for {name:" + name + "}");
        plugin.scheduleTaskAsync(runnable);
    }

    /**************************************************************************
     * Private Methods
     *************************************************************************/

    private PlayerEntry buildPlayerEntry(Player player, DeltaEssentialsPlayer dePlayer)
    {
        Preconditions.checkNotNull(dePlayer);

        PlayerPreSaveEvent preSaveEvent = new PlayerPreSaveEvent(player);
        Bukkit.getPluginManager().callEvent(preSaveEvent);

        PlayerEntry entry = new PlayerEntry(player.getName());
        SavedInventory inventory = new SavedInventory(player);

        entry.setHealth(player.getHealth());
        entry.setFoodLevel(player.getFoodLevel());
        entry.setXpLevel(player.getLevel());
        entry.setXpProgress(player.getExp());
        entry.setPotionEffects(player.getActivePotionEffects());
        entry.setEnderChest(player.getEnderChest().getContents());
        entry.setGameMode(player.getGameMode());
        entry.setSocialSpyEnabled(dePlayer.isSocialSpyEnabled());
        entry.setTeleportDenyEnabled(dePlayer.isTeleportDenyEnabled());
        entry.setLastReplyTarget(dePlayer.getLastReplyTarget());
        entry.setMetaData(preSaveEvent.getMetaData());

        if(player.hasPermission("DeltaEss.SingleInventory"))
        {
            entry.setGameMode(player.getGameMode());
            entry.setSurvival(inventory);
        }
        else if(player.getGameMode() == GameMode.SURVIVAL)
        {
            entry.setSurvival(inventory);
            entry.setCreative(dePlayer.getCreative());
        }
        else if(player.getGameMode() == GameMode.CREATIVE)
        {
            entry.setCreative(inventory);
            entry.setSurvival(dePlayer.getSurvival());
        }
        else
        {
            entry.setSurvival(dePlayer.getSurvival());
            entry.setCreative(dePlayer.getCreative());
        }
        return entry;
    }

    private void applyPlayerEntry(Player player, PlayerEntry entry, DeltaEssentialsPlayer dePlayer)
    {
        Settings settings = plugin.getSettings();
        GameMode defaultGameMode = settings.getDefaultGameMode();
        boolean isDefaultGameModeForced = settings.isDefaultGameModeForced();
        boolean bypassForced = player.hasPermission("DeltaInv.Forced.Bypass");

        dePlayer.setSurvival(entry.getSurvival());
        dePlayer.setCreative(entry.getCreative());
        dePlayer.setSocialSpyEnabled(entry.isSocialSpyEnabled());
        dePlayer.setTeleportDenyEnabled(entry.isTeleportDenyEnabled());
        dePlayer.setLastReplyTarget(entry.getLastReplyTarget());

        player.setHealth(entry.getHealth());
        player.setFoodLevel(entry.getFoodLevel());
        player.setLevel(entry.getXpLevel());
        player.setExp((float) entry.getXpProgress());
        player.getEnderChest().setContents(entry.getEnderChest());

        for(PotionEffect effect : player.getActivePotionEffects())
        {
            player.removePotionEffect(effect.getType());
        }

        for(PotionEffect effect : entry.getPotionEffects())
        {
            player.addPotionEffect(effect);
        }

        if(player.hasPermission("DeltaEss.SingleInventory"))
        {
            SavedInventory survival = dePlayer.getSurvival();
            player.getInventory().setContents(survival.getContents());
            player.getInventory().setArmorContents(survival.getArmor());
            dePlayer.setSurvival(null);
            dePlayer.setCreative(null);
        }
        else if(player.getGameMode() == GameMode.SURVIVAL)
        {
            SavedInventory survival = dePlayer.getSurvival();
            player.getInventory().setContents(survival.getContents());
            player.getInventory().setArmorContents(survival.getArmor());
            dePlayer.setSurvival(null);
        }
        else if(player.getGameMode() == GameMode.CREATIVE)
        {
            SavedInventory creative = dePlayer.getCreative();
            player.getInventory().setContents(creative.getContents());
            player.getInventory().setArmorContents(creative.getArmor());
            dePlayer.setCreative(null);
        }
        else
        {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
        }

        if(bypassForced || !isDefaultGameModeForced)
        {
            if(player.getGameMode() != entry.getGameMode())
            {
                player.setGameMode(entry.getGameMode());
            }
        }
        else if(player.getGameMode() != defaultGameMode)
        {
            player.setGameMode(defaultGameMode);
        }

        PlayerLoadedEvent loadedEvent = new PlayerLoadedEvent(player, entry.getMetaData());
        Bukkit.getPluginManager().callEvent(loadedEvent);
    }


//    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
//    public void onRedisMessageEvent(DeltaRedisMessageEvent event)
//    {
//        String channel = event.getChannel();
//
//        if(channel.equals(CommandMoveTo.MOVE_CHANNEL))
//        {
//            String[] splitMessage = pattern.split(event.getMessage(), 3);
//            String sender = splitMessage[0];
//            String nameToMove = splitMessage[1];
//            String destination = splitMessage[2];
//
//            Player playerToMove = Bukkit.getPlayer(nameToMove);
//            if(playerToMove != null && playerToMove.isOnline())
//            {
//                plugin.sendToServer(playerToMove, destination);
//                plugin.info(sender + " moved " + nameToMove + " to " + destination);
//            }
//        }
//        else if(channel.equals(CommandKick.KICK_CHANNEL))
//        {
//            String[] splitMessage = pattern.split(event.getMessage(), 3);
//            String sender = splitMessage[0];
//            String target = splitMessage[1];
//            String reason = splitMessage[2];
//
//            Player targetPlayer = Bukkit.getPlayer(target);
//            if(targetPlayer != null && targetPlayer.isOnline())
//            {
//                targetPlayer.kickPlayer(reason + "\n\n by " + ChatColor.GOLD + sender);
//            }
//
//            announceKick(sender, target, reason);
//        }
//        else if(channel.equals(CommandJail.JAIL_CHANNEL))
//        {
//            String[] splitMessage = pattern.split(event.getMessage(), 2);
//            String sender = splitMessage[0];
//            String command = splitMessage[1];
//
//            plugin.info(sender + " ran /" + command);
//            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
//        }
//    }
//
//    private void announceKick(String kicker, String playerKicked, String reason)
//    {
//        String announcement =
//            ChatColor.GOLD + kicker + ChatColor.WHITE + " kicked " +
//            ChatColor.GOLD + playerKicked + ChatColor.WHITE + " for " +
//            ChatColor.GOLD + reason;
//
//        for(Player onlinePlayer : Bukkit.getOnlinePlayers())
//        {
//            onlinePlayer.sendMessage(announcement);
//        }
//    }
}

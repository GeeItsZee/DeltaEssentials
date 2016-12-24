package com.gmail.tracebachi.DeltaEssentials.Listeners;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentials;
import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedPlayerInventory;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;
import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.formatNoPerm;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 7/21/16.
 */
public class PlayerGameModeListener extends DeltaEssentialsListener
{
    private LockedPlayerManager lockedPlayerManager;
    private Settings settings;

    public PlayerGameModeListener(LockedPlayerManager lockedPlayerManager, Settings settings,
                                  DeltaEssentials plugin)
    {
        super(plugin);

        Preconditions.checkNotNull(lockedPlayerManager, "lockedPlayerManager");
        Preconditions.checkNotNull(settings, "settings");

        this.lockedPlayerManager = lockedPlayerManager;
        this.settings = settings;
    }

    @Override
    public void shutdown()
    {
        this.lockedPlayerManager = null;
        this.settings = null;
        super.shutdown();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event)
    {
        Player player = event.getPlayer();
        String name = player.getName();
        GameMode currentMode = player.getGameMode();
        GameMode newMode = event.getNewGameMode();

        // Ignore game mode changes to the same mode
        if(currentMode == event.getNewGameMode()) { return; }

        DeltaEssPlayerData playerData = plugin.getPlayerDataMap().get(name);

        // If the player is not loaded, ignore the game mode change.
        if(playerData == null) { return; }

        // Prevent game mode change during loading or saving
        if(lockedPlayerManager.isLocked(name))
        {
            player.sendMessage(format("DeltaEss.PlayerLocked"));
            event.setCancelled(true);
            return;
        }

        boolean isNewGameModeDisabled = settings.getDisabledGameModes().contains(newMode);
        String gameModePerm = "DeltaEss.GameMode." + newMode.name();

        // If gamemode is disabled and player does not have bypass permission, prevent the change.
        if(isNewGameModeDisabled && !player.hasPermission(gameModePerm))
        {
            player.sendMessage(formatNoPerm(gameModePerm));
            event.setCancelled(true);
            return;
        }

        // If the player is sharing game mode inventories, ignore the game mode change.
        if(player.hasPermission("DeltaEss.SharedGameModeInv")) { return; }

        // Save the inventory associated with the old game mode
        if(currentMode == GameMode.SURVIVAL)
        {
            playerData.setSurvival(new SavedPlayerInventory(player));
        }
        else if(currentMode == GameMode.CREATIVE)
        {
            playerData.setCreative(new SavedPlayerInventory(player));
        }

        // Apply the inventory associated with the new game mode
        PlayerInventory playerInventory = player.getInventory();
        if(newMode == GameMode.SURVIVAL)
        {
            SavedPlayerInventory savedSurvival = playerData.getSurvival();
            playerInventory.setStorageContents(savedSurvival.getStorage());
            playerInventory.setArmorContents(savedSurvival.getArmor());
            playerInventory.setExtraContents(savedSurvival.getExtraSlots());

            playerData.clearSurvival();
        }
        else if(newMode == GameMode.CREATIVE)
        {
            SavedPlayerInventory savedCreative = playerData.getCreative();
            playerInventory.setStorageContents(savedCreative.getStorage());
            playerInventory.setArmorContents(savedCreative.getArmor());
            playerInventory.setExtraContents(savedCreative.getExtraSlots());
            playerData.setCreative(null);
        }
        else
        {
            playerInventory.clear();
            playerInventory.setArmorContents(new ItemStack[4]);
            playerInventory.setExtraContents(new ItemStack[1]);
        }
    }
}

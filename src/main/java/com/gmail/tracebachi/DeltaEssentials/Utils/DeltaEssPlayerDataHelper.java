package com.gmail.tracebachi.DeltaEssentials.Utils;

import com.gmail.tracebachi.DeltaEssentials.Settings;
import com.gmail.tracebachi.DeltaEssentials.Storage.DeltaEssPlayerData;
import com.gmail.tracebachi.DeltaEssentials.Storage.SavedPlayerInventory;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.google.common.base.Preconditions;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/23/16.
 */
public class DeltaEssPlayerDataHelper implements Shutdownable
{
    private static final String SHARED_GAMEMODE_INV_PERM = "DeltaEss.SharedGameModeInv";
    private static final String GAMEMODE_PERM_PREFIX = "DeltaEss.GameMode.";

    private Settings settings;

    public DeltaEssPlayerDataHelper(Settings settings)
    {
        Preconditions.checkNotNull(settings, "settings");
        this.settings = settings;
    }

    @Override
    public void shutdown()
    {
        this.settings = null;
    }

    /**
     * Applies all the passed data to the passed player
     * <p>
     * This method applies everything in the file to the player including
     * DeltaEssentials specific settings.
     * </p>
     *
     * @param playerData DeltaEssPlayerData to apply
     * @param player Player who will get the data
     */
    public void applyPlayerData(DeltaEssPlayerData playerData, Player player)
    {
        Preconditions.checkNotNull(playerData, "playerData");
        Preconditions.checkNotNull(player, "player");

        player.setHealth(playerData.getHealth());
        player.setFoodLevel(playerData.getFoodLevel());
        player.setLevel(playerData.getXpLevel());
        player.setExp(playerData.getXpProgress());
        player.getEnderChest().setContents(playerData.getEnderChest());

        if(!settings.shouldLoadAndSavePotionEffects())
        {
            for(PotionEffect effect : player.getActivePotionEffects())
            {
                player.removePotionEffect(effect.getType());
            }

            for(PotionEffect effect : playerData.getPotionEffects())
            {
                player.addPotionEffect(effect);
            }
        }

        PlayerInventory playerInventory = player.getInventory();
        if(player.hasPermission(SHARED_GAMEMODE_INV_PERM)||
            player.getGameMode() == GameMode.SURVIVAL)
        {
            SavedPlayerInventory survival = playerData.getSurvival();
            playerInventory.setStorageContents(survival.getStorage());
            playerInventory.setArmorContents(survival.getArmor());
            playerInventory.setExtraContents(survival.getExtraSlots());

            playerData.clearSurvival();
        }
        else if(player.getGameMode() == GameMode.CREATIVE)
        {
            SavedPlayerInventory creative = playerData.getCreative();
            playerInventory.setStorageContents(creative.getStorage());
            playerInventory.setArmorContents(creative.getArmor());
            playerInventory.setExtraContents(creative.getExtraSlots());

            playerData.clearCreative();
        }
        else
        {
            playerInventory.clear();
            playerInventory.setArmorContents(new ItemStack[4]);
            playerInventory.setExtraContents(new ItemStack[1]);
        }

        GameMode defaultGameMode = settings.getDefaultGameMode();
        if(!settings.shouldForceDefaultGameModeOnJoin() ||
            player.hasPermission(GAMEMODE_PERM_PREFIX + defaultGameMode))
        {
            if(player.getGameMode() != playerData.getGameMode())
            {
                player.setGameMode(playerData.getGameMode());
            }
        }
        else if(player.getGameMode() != defaultGameMode)
        {
            player.setGameMode(defaultGameMode);
        }
    }

    /**
     * Updates the passed data with information from the passed player
     * <p>
     * This method updates things like health, xp, and inventory which are
     * not constantly updated while the player is online.
     * </p>
     *
     * @param playerData DeltaEssPlayerData to update
     * @param player Player to get updated data from
     */
    public void updatePlayerData(DeltaEssPlayerData playerData, Player player)
    {
        Preconditions.checkNotNull(playerData, "playerData");
        Preconditions.checkNotNull(player, "player");

        playerData.setHealth(player.getHealth());
        playerData.setFoodLevel(player.getFoodLevel());
        playerData.setXpLevel(player.getLevel());
        playerData.setXpProgress(player.getExp());
        playerData.setGameMode(player.getGameMode());
        playerData.setPotionEffects(player.getActivePotionEffects());
        playerData.setEnderChest(player.getEnderChest().getContents());
        playerData.setHeldItemSlot(player.getInventory().getHeldItemSlot());

        if(!settings.shouldLoadAndSavePotionEffects())
        {
            playerData.setPotionEffects(player.getActivePotionEffects());
        }

        SavedPlayerInventory inventory = new SavedPlayerInventory(player);
        if(player.hasPermission(SHARED_GAMEMODE_INV_PERM) ||
            player.getGameMode() == GameMode.SURVIVAL)
        {
            playerData.setSurvival(inventory);
        }
        else if(player.getGameMode() == GameMode.CREATIVE)
        {
            playerData.setCreative(inventory);
        }
    }
}

package com.gmail.tracebachi.DeltaEssentials.Storage;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/19/16.
 */
public class DeltaEssentialsPlayer
{
    private boolean socialSpyEnabled;
    private boolean teleportDenyEnabled;
    private String lastReplyTarget;
    private SavedInventory survival;
    private SavedInventory creative;

    public boolean isSocialSpyEnabled()
    {
        return socialSpyEnabled;
    }

    public void setSocialSpyEnabled(boolean socialSpyEnabled)
    {
        this.socialSpyEnabled = socialSpyEnabled;
    }

    public boolean isTeleportDenyEnabled()
    {
        return teleportDenyEnabled;
    }

    public void setTeleportDenyEnabled(boolean teleportDenyEnabled)
    {
        this.teleportDenyEnabled = teleportDenyEnabled;
    }

    public String getLastReplyTarget()
    {
        return (lastReplyTarget != null) ? lastReplyTarget : "";
    }

    public void setLastReplyTarget(String lastReplyTarget)
    {
        this.lastReplyTarget = lastReplyTarget;
    }

    public SavedInventory getSurvival()
    {
        return (survival != null) ? survival : SavedInventory.EMPTY;
    }

    public void setSurvival(SavedInventory survival)
    {
        this.survival = survival;
    }

    public SavedInventory getCreative()
    {
        return (creative != null) ? creative : SavedInventory.EMPTY;
    }

    public void setCreative(SavedInventory creative)
    {
        this.creative = creative;
    }
}

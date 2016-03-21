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
package com.gmail.tracebachi.DeltaEssentials.Storage;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/19/16.
 */
public class DeltaEssPlayerData
{
    private boolean socialSpyEnabled;
    private boolean teleportDenyEnabled;
    private boolean vanishEnabled;
    private String replyTo;
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

    public boolean isVanishEnabled()
    {
        return vanishEnabled;
    }

    public void setVanishEnabled(boolean vanishEnabled)
    {
        this.vanishEnabled = vanishEnabled;
    }

    public String getReplyTo()
    {
        return (replyTo != null) ? replyTo : "";
    }

    public void setReplyTo(String replyTo)
    {
        this.replyTo = replyTo;
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

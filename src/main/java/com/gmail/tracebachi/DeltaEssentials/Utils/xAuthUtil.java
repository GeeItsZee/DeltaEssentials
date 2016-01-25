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
package com.gmail.tracebachi.DeltaEssentials.Utils;

import de.luricos.bukkit.xAuth.event.command.player.xAuthCommandRegisterEvent;
import de.luricos.bukkit.xAuth.event.xAuthEvent;
import de.luricos.bukkit.xAuth.event.xAuthEventProperties;

import java.lang.reflect.Field;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/20/16.
 */
public class xAuthUtil
{
    private static Field propertiesField;

    static
    {
        try
        {
            propertiesField = xAuthEvent.class.getDeclaredField("properties");
            propertiesField.setAccessible(true);
        }
        catch(NoSuchFieldException e)
        {
            e.printStackTrace();
            propertiesField = null;
        }
    }

    public static String getPlayerNameFromRegisterEvent(xAuthCommandRegisterEvent event)
    {
        try
        {
            xAuthEventProperties properties = ((xAuthEventProperties) propertiesField.get(event));
            String name = (String) properties.getProperty("playername");
            return (name == null) ? null : name.toLowerCase();
        }
        catch(IllegalAccessException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }
}

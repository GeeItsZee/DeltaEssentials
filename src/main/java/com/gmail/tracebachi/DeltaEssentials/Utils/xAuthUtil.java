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

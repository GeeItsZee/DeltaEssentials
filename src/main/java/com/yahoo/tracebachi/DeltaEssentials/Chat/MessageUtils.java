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
package com.yahoo.tracebachi.DeltaEssentials.Chat;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;

import java.nio.charset.StandardCharsets;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public interface MessageUtils
{
    static String format(String sender, String receiver, String message, boolean allowColors)
    {
        String prefix = ChatColor.translateAlternateColorCodes('&',
            "&8[&dPM&8]&7=&8[&e" + sender + " -> " + receiver + "&8]&d ");

        if(allowColors)
        {
            return prefix + ChatColor.translateAlternateColorCodes('&', message);
        }
        else
        {
            return prefix + message;
        }
    }

    static String toByteArrayDataString(String sender, String receiver, boolean allowColors, String message)
    {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(sender);
        output.writeUTF(receiver);
        output.writeUTF(message);
        output.writeBoolean(allowColors);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}

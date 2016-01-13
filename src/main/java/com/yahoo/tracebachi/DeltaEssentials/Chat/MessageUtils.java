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

import java.nio.charset.StandardCharsets;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 11/29/15.
 */
public interface MessageUtils
{
    static String formatForLog(String sender, String receiver, String message)
    {
        return "[" + sender + " -> " + receiver + "] " + message;
    }

    static String formatForSpy(String sender, String receiver, String message)
    {
        return "\u00A78[\u00A77Spy\u00A78]\u00A77=\u00A78[" +
            "\u00A77" + sender + " \u00A78-> " +
            "\u00A77" + receiver + "\u00A78] \u00A77" + message;
    }

    static String formatForSender(String receiver, String message)
    {
        return "\u00A78[\u00A7dPM\u00A78]\u00A77=\u00A78[\u00A7eme -> " +
            receiver + "\u00A78]\u00A7d " + message;
    }

    static String formatForReceiver(String sender, String message)
    {
        return "\u00A78[\u00A7dPM\u00A78]\u00A77=\u00A78[\u00A7e" +
            sender + " -> me\u00A78]\u00A7d " + message;
    }

    static String toByteArrayDataString(String sender, String receiver, String message)
    {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(sender);
        output.writeUTF(receiver);
        output.writeUTF(message);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}

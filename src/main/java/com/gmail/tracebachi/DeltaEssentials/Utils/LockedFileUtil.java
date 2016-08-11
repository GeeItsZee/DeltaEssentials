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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/21/16.
 */
public interface LockedFileUtil
{
    static String read(File file) throws IOException
    {
        return read(file, StandardCharsets.UTF_8);
    }

    static String read(File file, Charset charset) throws IOException
    {
        FileLock lock = null;
        FileChannel fileChannel = null;

        try
        {
            fileChannel = FileChannel.open(
                file.toPath(),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);

            // Lock the file
            lock = fileChannel.lock();

            ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
            fileChannel.read(buffer);

            return new String(buffer.array(), charset);
        }
        finally
        {
            // Release the file lock
            if(lock != null)
            {
                try
                {
                    lock.release();
                }
                catch(IOException ex)
                {
                    ex.printStackTrace();
                }
            }

            // Close the file channel
            if(fileChannel != null)
            {
                try
                {
                    fileChannel.close();
                }
                catch(IOException ex)
                {
                    ex.printStackTrace();
                }
            }

            // Note: Do NOT return from a finally-block
        }
    }

    static boolean write(String toWrite, File file) throws IOException
    {
        return write(toWrite, file, StandardCharsets.UTF_8);
    }

    static boolean write(String toWrite, File file, Charset charset) throws IOException
    {
        FileLock lock = null;
        FileChannel fileChannel = null;

        try
        {
            if(!file.exists() && !file.createNewFile())
            {
                return false;
            }

            fileChannel = FileChannel.open(
                file.toPath(),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);
            ByteBuffer buffer = ByteBuffer.wrap(toWrite.getBytes(charset));

            // Lock the file
            lock = fileChannel.lock();

            // Truncate the file (delete the current contents)
            fileChannel.truncate(0);
            fileChannel.write(buffer);

            return true;
        }
        finally
        {
            // Release the file lock
            if(lock != null)
            {
                try
                {
                    lock.release();
                }
                catch(IOException ex)
                {
                    ex.printStackTrace();
                }
            }

            // Close the file channel
            if(fileChannel != null)
            {
                try
                {
                    fileChannel.close();
                }
                catch(IOException ex)
                {
                    ex.printStackTrace();
                }
            }

            // Note: Do NOT return from a finally-block
        }
    }
}

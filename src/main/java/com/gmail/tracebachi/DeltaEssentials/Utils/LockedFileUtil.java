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
        return read(file, StandardCharsets.UTF_16);
    }

    static String read(File file, Charset charset) throws IOException
    {
        FileLock lock = null;

        try
        {
            FileChannel fileChannel = FileChannel.open(file.toPath(),
                StandardOpenOption.READ, StandardOpenOption.WRITE);

            // Lock the file
            lock = fileChannel.lock();

            ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
            fileChannel.read(buffer);

            // Unlock the file
            lock.release();
            return new String(buffer.array(), charset);
        }
        finally
        {
            if(lock != null)
            {
                lock.release();
            }
            // Note: Do NOT return from a finally-block
        }
    }

    static boolean write(String toWrite, File file) throws IOException
    {
        return write(toWrite, file, StandardCharsets.UTF_16);
    }

    static boolean write(String toWrite, File file, Charset charset) throws IOException
    {
        FileLock lock = null;

        try
        {
            if(!file.exists() && !file.createNewFile())
            {
                return false;
            }

            FileChannel fileChannel = FileChannel.open(file.toPath(),
                StandardOpenOption.READ, StandardOpenOption.WRITE);
            ByteBuffer buffer = ByteBuffer.wrap(toWrite.getBytes(charset));

            // Lock the file
            lock = fileChannel.lock();

            // Truncate the file (delete the current contents)
            fileChannel.truncate(0);
            fileChannel.write(buffer);

            // Unlock the file
            lock.release();
            return true;
        }
        finally
        {
            if(lock != null)
            {
                lock.release();
            }
            // Note: Do NOT return from a finally-block
        }
    }
}

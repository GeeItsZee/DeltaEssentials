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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/12/15.
 */
public interface CompressionUtils
{
    static byte[] compress(byte[] decompressed) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(decompressed);
        gzip.flush();
        gzip.close();
        bos.flush();
        bos.close();
        return bos.toByteArray();
    }

    static byte[] decompress(byte[] compressed) throws IOException
    {
        int nRead;
        byte[] data = new byte[2048];
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        GZIPInputStream gzip = new GZIPInputStream(bis);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        while((nRead = gzip.read(data, 0, data.length)) != -1)
        {
            buffer.write(data, 0, nRead);
        }

        gzip.close();
        bis.close();
        return buffer.toByteArray();
    }
}

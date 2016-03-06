package com.gmail.tracebachi.DeltaEssentials;

import com.gmail.tracebachi.DeltaEssentials.Listeners.PlayerLockListener;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Structures.CaseInsensitiveHashMap;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 3/5/16.
 */
public class TimedPlayerLockManager implements Shutdownable
{
    private CaseInsensitiveHashMap<Long> playerMap = new CaseInsensitiveHashMap<>();
    private PlayerLockListener lockListener;

    public TimedPlayerLockManager(PlayerLockListener lockListener)
    {
        this.lockListener = lockListener;
    }

    @Override
    public void shutdown()
    {
        playerMap.clear();
        playerMap = null;
        lockListener = null;
    }

    public void add(String name)
    {
        playerMap.put(name, System.currentTimeMillis() + 15000);
        lockListener.add(name);
    }

    public Long remove(String name)
    {
        lockListener.remove(name);
        return playerMap.remove(name);
    }

    public Long get(String name)
    {
        return playerMap.get(name);
    }

    public void clear()
    {
        playerMap.clear();
    }

    public void cleanup()
    {
        Iterator<Map.Entry<String, Long>> iter = playerMap.entrySet().iterator();
        Long current = System.currentTimeMillis();

        while(iter.hasNext())
        {
            Map.Entry<String, Long> entry = iter.next();
            String name = entry.getKey();
            Long endTime = entry.getValue();

            if(endTime < current)
            {
                lockListener.remove(name);
                iter.remove();
            }
        }
    }
}

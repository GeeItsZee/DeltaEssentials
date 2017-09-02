/*
 * DeltaEssentials - Player data, chat, and teleport plugin for BungeeCord and Spigot servers
 * Copyright (C) 2017 tracebachi@gmail.com (GeeItsZee)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaEssentials.Bungee;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Channels;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessageNotifier;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerFileLockListener implements Registerable
{
  private final SockExchangeApi api;
  private final HashMap<String, PlayerFileLockOwner> playerFileLockerOwnerMap = new HashMap<>();
  private final Consumer<ReceivedMessage> lockRequestListener;
  private final Consumer<ReceivedMessage> lockReleaseListener;
  private ScheduledFuture<?> cleanupLocksFuture;

  public PlayerFileLockListener(SockExchangeApi api)
  {
    Preconditions.checkNotNull(api, "api");

    this.api = api;
    this.lockRequestListener = this::onLockRequest;
    this.lockReleaseListener = this::onLockRelease;
  }

  @Override
  public void register()
  {
    ReceivedMessageNotifier notifier = api.getMessageNotifier();
    notifier.register(Channels.PLAYER_FILE_LOCK_REQUEST, lockRequestListener);
    notifier.register(Channels.PLAYER_FILE_LOCK_RELEASE, lockReleaseListener);

    cleanupLocksFuture = api.getScheduledExecutorService().scheduleAtFixedRate(
      this::cleanupExpiredLocks, 5, 5, TimeUnit.MINUTES);
  }

  @Override
  public void unregister()
  {
    if (cleanupLocksFuture != null)
    {
      cleanupLocksFuture.cancel(false);
      cleanupLocksFuture = null;
    }

    ReceivedMessageNotifier notifier = api.getMessageNotifier();
    notifier.unregister(Channels.PLAYER_FILE_LOCK_REQUEST, lockRequestListener);
    notifier.unregister(Channels.PLAYER_FILE_LOCK_RELEASE, lockReleaseListener);
  }

  private void onLockRequest(ReceivedMessage message)
  {
    ByteArrayDataInput in = message.getDataInput();
    int nameCount = in.readInt();
    String sourceServerName = in.readUTF();
    ByteArrayDataOutput out = ByteStreams.newDataOutput(nameCount * 40);
    long currentMillis = System.currentTimeMillis();

    out.writeInt(nameCount);

    synchronized (playerFileLockerOwnerMap)
    {
      for (int i = 0; i < nameCount; i++)
      {
        String playerName = in.readUTF().toLowerCase();
        PlayerFileLockOwner lockOwner = playerFileLockerOwnerMap.get(playerName);

        out.writeUTF(playerName);

        // If there is no lock owner, the lock is expired, or the requesting server already
        // has the lock, then the requesting server can have the lock.
        if (lockOwner == null || lockOwner.getExpiresAtMillis() <= currentMillis ||
          sourceServerName.equalsIgnoreCase(lockOwner.getOwnerServerName()))
        {
          long newExpiresAtMillis = currentMillis + TimeUnit.MINUTES.toMillis(1);
          PlayerFileLockOwner newPlayerFileLockOwner = new PlayerFileLockOwner(sourceServerName,
            newExpiresAtMillis);

          playerFileLockerOwnerMap.put(playerName, newPlayerFileLockOwner);

          out.writeBoolean(true);
        }
        else
        {
          out.writeBoolean(false);
        }
      }
    }

    // Respond to the source server
    message.respond(out.toByteArray());
  }

  private void onLockRelease(ReceivedMessage message)
  {
    ByteArrayDataInput in = message.getDataInput();
    int nameCount = in.readInt();
    String sourceServerName = in.readUTF();

    synchronized (playerFileLockerOwnerMap)
    {
      for (int i = 0; i < nameCount; i++)
      {
        String playerName = in.readUTF().toLowerCase();
        PlayerFileLockOwner lockOwner = playerFileLockerOwnerMap.get(playerName);

        // Release the lock only if the owner is the source server
        if (lockOwner != null && lockOwner.getOwnerServerName().equalsIgnoreCase(sourceServerName))
        {
          playerFileLockerOwnerMap.remove(playerName);
        }
      }
    }
  }

  private void cleanupExpiredLocks()
  {
    synchronized (playerFileLockerOwnerMap)
    {
      long currentMillis = System.currentTimeMillis();

      // Remove if there is no lock owner or the lock owner's lock has expired
      playerFileLockerOwnerMap.values().removeIf(
        lockOwner -> lockOwner == null || lockOwner.getExpiresAtMillis() <= currentMillis);
    }
  }

  private static class PlayerFileLockOwner
  {
    private final String ownerServerName;
    private final long expiresAtMillis;

    private PlayerFileLockOwner(String ownerServerName, long expiresAtMillis)
    {
      this.ownerServerName = ownerServerName;
      this.expiresAtMillis = expiresAtMillis;
    }

    private String getOwnerServerName()
    {
      return ownerServerName;
    }

    private long getExpiresAtMillis()
    {
      return expiresAtMillis;
    }
  }
}

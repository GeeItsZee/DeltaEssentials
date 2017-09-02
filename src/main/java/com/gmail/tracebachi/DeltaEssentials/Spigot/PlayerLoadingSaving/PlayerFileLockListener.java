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
package com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerLoadingSaving;

import com.gmail.tracebachi.DeltaEssentials.DeltaEssentialsConstants.Channels;
import com.gmail.tracebachi.SockExchange.Messages.ResponseMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.CaseInsensitiveMap;
import com.gmail.tracebachi.SockExchange.Utilities.ExtraPreconditions;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerFileLockListener implements Registerable
{
  private static final long REQUEST_LOCK_RATE = TimeUnit.SECONDS.toMillis(1);
  private static final long REQUEST_LOCK_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
  private static final int MAX_RETRIES_FOR_LOCK_REQUEST = 60;

  private final SockExchangeApi api;
  private final Executor executor;
  private final CaseInsensitiveMap<List<RetriableConsumer>> nameLockConsumerMap;
  private final Random random = new Random();
  private ScheduledFuture<?> requestLocksFuture;
  private int requestAllCountdown;
  private boolean registered = false;

  public PlayerFileLockListener(SockExchangeApi api, Executor executor)
  {
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(executor, "executor");

    this.api = api;
    this.executor = executor;
    this.nameLockConsumerMap = new CaseInsensitiveMap<>(new HashMap<>());
  }

  @Override
  public void register()
  {
    ScheduledExecutorService executorService = api.getScheduledExecutorService();

    // NameLockConsumerMap is used as a lock object
    synchronized (nameLockConsumerMap)
    {
      // There is nothing to do if the listener is registered
      if (registered)
      {
        return;
      }

      // Reschedule the task and mark the listener as registered
      requestLocksFuture = executorService.scheduleAtFixedRate(
        this::sendRequestForLocks, REQUEST_LOCK_RATE, REQUEST_LOCK_RATE,
        TimeUnit.MILLISECONDS);
      registered = true;
    }
  }

  @Override
  public void unregister()
  {
    Set<String> locksToRelease = new HashSet<>();

    // NameLockConsumerMap is used as a lock object
    synchronized (nameLockConsumerMap)
    {
      // There is nothing to do if the listener is unregistered
      if (!registered)
      {
        return;
      }

      for (Map.Entry<String, List<RetriableConsumer>> entry : nameLockConsumerMap.entrySet())
      {
        String playerName = entry.getKey();
        List<RetriableConsumer> consumerList = entry.getValue();

        // Run all consumers as if the lock request failed
        for (RetriableConsumer consumer : consumerList)
        {
          executor.execute(() -> consumer.accept(false));
        }

        locksToRelease.add(playerName);
      }

      // Cancel the task, clear the consumers, and mark the listener as unregistered
      requestLocksFuture.cancel(false);
      requestLocksFuture = null;
      nameLockConsumerMap.clear();
      registered = false;
    }

    String serverName = api.getServerName();
    byte[] messageBytes = getMessageBytesForLockRelease(serverName, locksToRelease);

    api.sendToBungee(Channels.PLAYER_FILE_LOCK_RELEASE, messageBytes);
  }

  public void requestLock(String playerName, Consumer<Boolean> consumer)
  {
    ExtraPreconditions.checkNotEmpty(playerName, "playerName");
    Preconditions.checkNotNull(consumer, "consumer");

    // NameLockConsumerMap is used as a lock object
    synchronized (nameLockConsumerMap)
    {
      // If the listener is unregistered, treat all new requests as failing.
      if (!registered)
      {
        executor.execute(() -> consumer.accept(false));
        return;
      }

      List<RetriableConsumer> consumerList = nameLockConsumerMap.computeIfAbsent(
        playerName, k -> new ArrayList<>(2));

      consumerList.add(new RetriableConsumer(consumer));
    }
  }

  public void releaseLock(String playerName)
  {
    ExtraPreconditions.checkNotEmpty(playerName, "playerName");

    List<RetriableConsumer> consumerList;

    // NameLockConsumerMap is used as a lock object
    synchronized (nameLockConsumerMap)
    {
      consumerList = nameLockConsumerMap.remove(playerName);
    }

    // If there was no list in the map, there is nothing to do.
    if (consumerList == null)
    {
      return;
    }

    // Run all consumers as if the lock request failed
    for (RetriableConsumer retriableConsumer : consumerList)
    {
      executor.execute(() -> retriableConsumer.accept(false));
    }

    String serverName = api.getServerName();
    Set<String> nameSet = Collections.singleton(playerName);
    byte[] messageBytes = getMessageBytesForLockRelease(serverName, nameSet);

    api.sendToBungee(Channels.PLAYER_FILE_LOCK_RELEASE, messageBytes);
  }

  private void sendRequestForLocks()
  {
    Set<String> locksToRequest;

    // NameLockConsumerMap is used as a lock object
    synchronized (nameLockConsumerMap)
    {
      // There is nothing to do if the listener is unregistered
      if (!registered)
      {
        return;
      }

      boolean shouldRequestAllLocks = shouldRequestAllLocks();
      locksToRequest = new HashSet<>(nameLockConsumerMap.size());

      for (Map.Entry<String, List<RetriableConsumer>> entry : nameLockConsumerMap.entrySet())
      {
        if (shouldRequestAllLocks || !entry.getValue().isEmpty())
        {
          locksToRequest.add(entry.getKey());
        }
      }
    }

    // No need to request if there are no locks to request
    if (locksToRequest.isEmpty())
    {
      return;
    }

    String serverName = api.getServerName();
    byte[] messageBytes = getMessageBytesForLockRequest(serverName, locksToRequest);

    api.sendToBungee(
      Channels.PLAYER_FILE_LOCK_REQUEST, messageBytes, this::onLockRequestResponse,
      REQUEST_LOCK_TIMEOUT);
  }

  private void onLockRequestResponse(ResponseMessage message)
  {
    // There is nothing to do if the SockExchange request failed.
    if (!message.getResponseStatus().isOk())
    {
      return;
    }

    ByteArrayDataInput in = message.getDataInput();
    int playerNameCount = in.readInt();

    // NameLockConsumerMap is used as a lock object
    synchronized (nameLockConsumerMap)
    {
      for (int i = 0; i < playerNameCount; i++)
      {
        String playerName = in.readUTF();
        boolean result = in.readBoolean();
        List<RetriableConsumer> consumerList = nameLockConsumerMap.get(playerName);

        // Null lists are not allowed, so remove the mapping
        if (consumerList == null)
        {
          nameLockConsumerMap.remove(playerName);
          continue;
        }

        Iterator<RetriableConsumer> iterator = consumerList.iterator();

        while (iterator.hasNext())
        {
          RetriableConsumer retriableConsumer = iterator.next();

          if (!result && retriableConsumer.decrementAndGetRemainingRetries() > 0)
          {
            continue;
          }

          // Remove and execute async
          iterator.remove();
          executor.execute(() -> retriableConsumer.accept(result));
        }
      }
    }
  }

  private boolean shouldRequestAllLocks()
  {
    // Approximately every 30 - 45 counts, request all locks and reset the countdown.
    // This is to avoid multiple servers requesting all locks around the same time, which is
    // a rare case. This pseudo-randomness will help prevent that from happening often.
    if (--requestAllCountdown <= 0)
    {
      // Generate a new countdown
      requestAllCountdown = 30 + random.nextInt(16);
      return true;
    }

    return false;
  }

  private static byte[] getMessageBytesForLockRelease(String serverName, Set<String> playerNames)
  {
    int estimatedSize = (playerNames.size() + 1) * 40;
    ByteArrayDataOutput out = ByteStreams.newDataOutput(estimatedSize);
    out.writeInt(playerNames.size());
    out.writeUTF(serverName);

    for (String playerName : playerNames)
    {
      out.writeUTF(playerName.toLowerCase());
    }

    return out.toByteArray();
  }

  private static byte[] getMessageBytesForLockRequest(String serverName, Set<String> playerNames)
  {
    int estimatedSize = (playerNames.size() + 1) * 40;
    ByteArrayDataOutput out = ByteStreams.newDataOutput(estimatedSize);
    out.writeInt(playerNames.size());
    out.writeUTF(serverName);

    for (String playerName : playerNames)
    {
      out.writeUTF(playerName.toLowerCase());
    }

    return out.toByteArray();
  }

  private static class RetriableConsumer implements Consumer<Boolean>
  {
    private final Consumer<Boolean> consumer;
    private int remainingRetries = MAX_RETRIES_FOR_LOCK_REQUEST;

    private RetriableConsumer(Consumer<Boolean> consumer)
    {
      this.consumer = consumer;
    }

    @Override
    public void accept(Boolean aBoolean)
    {
      consumer.accept(aBoolean);
    }

    private int decrementAndGetRemainingRetries()
    {
      return --remainingRetries;
    }
  }
}

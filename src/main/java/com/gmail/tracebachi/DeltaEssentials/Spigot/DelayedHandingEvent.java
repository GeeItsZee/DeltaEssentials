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
package com.gmail.tracebachi.DeltaEssentials.Spigot;

import com.google.common.base.Preconditions;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class DelayedHandingEvent<T extends DelayedHandingEvent> extends Event
{
  private final Consumer<T> consumer;
  private final AtomicInteger intentCounter = new AtomicInteger(1);

  public DelayedHandingEvent(Consumer<T> consumer)
  {
    Preconditions.checkNotNull(consumer, "consumer");

    this.consumer = consumer;
  }

  public int registerIntent()
  {
    return intentCounter.updateAndGet((current) ->
    {
      if (current == 0)
      {
        // Prevent incrementing if 0 because the consumer should have already run if 0
        throw new IllegalStateException("Event has already completed");
      }
      else
      {
        // Atomic increment
        return current + 1;
      }
    });
  }

  public int completeIntent()
  {
    int updated = intentCounter.updateAndGet((current) ->
    {
      if (current == 0)
      {
        // Prevent decrementing if 0 because the consumer should have already run if 0
        throw new IllegalStateException(
          "Event is not waiting. Did someone call this method one too many times?");
      }
      else
      {
        // Atomic decrement
        return current - 1;
      }
    });

    if (updated == 0)
    {
      consumer.accept((T) this);
    }

    return updated;
  }

  /* START Required by Spigot *********************************************************************/

  private static final HandlerList handlers = new HandlerList();

  public HandlerList getHandlers()
  {
    return handlers;
  }

  public static HandlerList getHandlerList()
  {
    return handlers;
  }

  /* END Required by Spigot ***********************************************************************/
}

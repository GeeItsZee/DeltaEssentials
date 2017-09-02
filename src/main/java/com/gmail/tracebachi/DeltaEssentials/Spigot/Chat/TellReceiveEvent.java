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
package com.gmail.tracebachi.DeltaEssentials.Spigot.Chat;

import com.gmail.tracebachi.DeltaEssentials.Spigot.DelayedHandingEvent;
import com.google.common.base.Preconditions;
import org.bukkit.event.HandlerList;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TellReceiveEvent extends DelayedHandingEvent<TellReceiveEvent>
{
  private final String senderName;
  private final String receiverName;
  private final boolean ignoreVanish;
  private final ConcurrentHashMap<String, byte[]> extraDataMap;
  private String message;
  private boolean cancelled;
  private String cancelMessage;

  public TellReceiveEvent(
    String senderName, String receiverName, boolean ignoreVanish,
    ConcurrentHashMap<String, byte[]> extraDataMap, String message,
    Consumer<TellReceiveEvent> consumer)
  {
    super(consumer);

    Preconditions.checkNotNull(senderName, "senderName");
    Preconditions.checkNotNull(receiverName, "receiverName");
    Preconditions.checkNotNull(extraDataMap, "extraDataMap");
    Preconditions.checkNotNull(message, "message");

    this.senderName = senderName;
    this.receiverName = receiverName;
    this.ignoreVanish = ignoreVanish;
    this.extraDataMap = extraDataMap;
    this.message = message;
  }

  public String getSenderName()
  {
    return senderName;
  }

  public String getReceiverName()
  {
    return receiverName;
  }

  public boolean canIgnoreVanish()
  {
    return ignoreVanish;
  }

  public ConcurrentHashMap<String, byte[]> getExtraDataMap()
  {
    return extraDataMap;
  }

  public String getMessage()
  {
    return message;
  }

  public void setMessage(String message)
  {
    Preconditions.checkNotNull(message, "message");
    this.message = message;
  }

  public boolean isCancelled()
  {
    return cancelled;
  }

  public void setCancelled(boolean cancelled)
  {
    this.cancelled = cancelled;
  }

  public String getCancelMessage()
  {
    return cancelMessage;
  }

  public void setCancelMessage(String cancelMessage)
  {
    this.cancelMessage = cancelMessage;
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

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

import com.gmail.tracebachi.DeltaEssentials.Spigot.DeltaEssentialsPlugin;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.google.common.base.Preconditions;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
abstract class AbstractTellCommand
{
  protected static final String COMMAND_PERM = "DeltaEss.Tell";
  protected static final String TELL_USE_COLOR_PERM = "DeltaEss.Tell.Color";
  protected static final String TELL_IGNORE_VANISH_PERM = "DeltaEss.Tell.IgnoreVanish";

  protected final DeltaEssentialsPlugin plugin;
  protected final TellChatManager tellChatManager;
  protected final MessageFormatMap formatMap;
  protected final SockExchangeApi api;

  public AbstractTellCommand(
    DeltaEssentialsPlugin plugin, TellChatManager tellChatManager, MessageFormatMap formatMap,
    SockExchangeApi api)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(tellChatManager, "tellChatManager");
    Preconditions.checkNotNull(formatMap, "formatMap");
    Preconditions.checkNotNull(api, "api");

    this.plugin = plugin;
    this.tellChatManager = tellChatManager;
    this.formatMap = formatMap;
    this.api = api;
  }
}

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

import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;

import java.util.ArrayList;
import java.util.List;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TabCompleteNameHelper
{
  public static List<String> getNamesThatStartsWith(String partialName, SockExchangeApi api)
  {
    List<String> results = new ArrayList<>(2);

    partialName = partialName.toLowerCase();

    for (String name : api.getOnlinePlayerNames())
    {
      if (name.toLowerCase().startsWith(partialName))
      {
        results.add(name);
      }
    }

    return results;
  }
}

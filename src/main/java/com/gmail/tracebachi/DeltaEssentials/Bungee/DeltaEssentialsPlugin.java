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

import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class DeltaEssentialsPlugin extends Plugin
{
  private PlayerFileLockListener playerFileLockListener;

  @Override
  public void onEnable()
  {
    SockExchangeApi api = SockExchangeApi.instance();

    playerFileLockListener = new PlayerFileLockListener(api);
    playerFileLockListener.register();
  }

  @Override
  public void onDisable()
  {
    if (playerFileLockListener != null)
    {
      playerFileLockListener.unregister();
      playerFileLockListener = null;
    }
  }
}

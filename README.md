# DeltaEssentials
DeltaEssentials is a plugin for Bukkit / Spigot using BungeeCord. It has functionality similar
to the popular Essentials plugin, but adds multi-server support. DeltaEssentials requires
[DeltaRedis](https://github.com/geeitszee/DeltaRedis) for communication and 
[DeltaExecutor](https://github.com/geeitszee/DeltaExecutor) for scheduling.

## Permissions
```yml
DeltaEss.Debug: Access to /deltaessdebug
DeltaEss.Disposal: Access to /disposal
DeltaEss.DVanish: Access to /dvanish
DeltaEss.Lockdown: Access to /lockdown

DeltaEss.MoveAll: Access to /moveall
DeltaEss.MoveTo.Self: Access to /mt <Server Name>
DeltaEss.MoveTo.Other: Access to /mt <Server Name> <Player>
DeltaEss.MoveToBlocked.<Server Name>: Access to /mt <Server Name> if server is blocked

DeltaEss.SocialSpy: Access to /socialspy

DeltaEss.Tell.Use: Access to /tell and /reply
DeltaEss.Tell.Color: Access to color codes in PMs
DeltaEss.Tell.IgnoreVanish: Access to PM even when other player is vanished

DeltaEss.Tp.Self: Access to /tp <Player>
DeltaEss.Tp.Other: Access to /tp <Player> <Player> and /tphere
DeltaEss.Tp.Deny: Access to /tpdeny
DeltaEss.Tp.IgnoreVanish: Ignores if player is vanished when /tp is used.
DeltaEss.Tp.IgnoreDeny: Ignores if player is denying teleports when /tp is used.
DeltaEss.Tp.Silent: Target player is not alerted when user teleports.

DeltaEss.Tpa.Send: Access to /tpahere
DeltaEss.Tpa.Accept: Access to /tpaccept

DeltaEss.SharedGameModeInv: Inventory is shared between gamemodes
DeltaEss.GameMode.<GameMode>: Access to switch to or be in <GameMode>.
```

# Licence ([GPLv3](http://www.gnu.org/licenses/gpl-3.0.en.html))
```
DeltaEssentials - Basic server functionality for Bukkit/Spigot servers using BungeeCord.
Copyright (C) 2015  Trace Bachi (tracebachi@gmail.com)

DeltaEssentials is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

DeltaEssentials is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with DeltaEssentials.  If not, see <http://www.gnu.org/licenses/>.
```

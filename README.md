# DeltaEssentials
Plugin for essential server functionality like player data syncing, chat, teleport, and cross-server vanish
for BungeeCord and Spigot servers built using [SockExchange](https://github.com/geeitszee/SockExchange)

## Installation
Copy the same JAR into the plugins directory of your BungeeCord and Spigot installations. The
[default Spigot configuration](https://github.com/GeeItsZee/DeltaEssentials/blob/3.0.0-rewrite-using-sockexchange/src/main/resources/config.yml)
should be fine.

In order for player data sharing to work, you will need to create a symbolic link for the
`plugins/DeltaEssentials/PlayerData` folder with all other servers that should share the player data.
For example, if `Castle` and `Fort` servers should share player data, create a symbolic link where
`Castle/plugins/DeltaEssentials/PlayerData` and `Fort/plugins/DeltaEssentials/PlayerData` point to the
same folder. (Technical detail: DeltaEssentials handles file locking using Bungee, so only one server
at a time should be able to read/write a player file)

## Commands
`/tell` and `/reply`
- Permissions: 
  - `DeltaEss.Tell` for normal tell
  - `DeltaEss.Tell.Color` to allow color codes
  - `DeltaEss.Tell.IgnoreVanish` to send a message even if the player is vanished
- Description: Standard commands for personal messages

`/socialspy`
- Permission: `DeltaEss.SocialSpy`
- Description: Moderating command for viewing personal messages as they are sent

`/blocktp`
- Permission: `DeltaEss.BlockTp`
- Description: Blocks players from teleporting to you

`/tp`
- Permissions: 
  - `DeltaEss.Tp.MeToOther` for the normal command
  - `DeltaEss.Tp.OtherToOther` for teleporting other players 
  - `DeltaEss.Tp.IgnoreVanish` to teleport even if the player is vanished
  - `DeltaEss.Tp.IgnoreBlocking` to teleport even if the player is blocking teleports
- Description: Teleports you to the target player or the player you specify to the target player

`/tphere`
- Permission: `DeltaEss.Tp.OtherToMe` (ignores if a player is vanished or blocking)
- Description: Teleports the target player to you

`/tpahere`
- Permissions: 
  - `DeltaEss.Tp.OtherToMe.Ask` for the normal command
  - `DeltaEss.Tp.IgnoreVanish` to teleport even if the player is vanished
  - `DeltaEss.Tp.IgnoreBlocking` to teleport even if the player is blocking teleports
- Description: Sends a teleport request which will allow the target player to teleport to you

`/tpaccept` and `/tpdeny`
- Permission: `DeltaEss.Tp.OtherToMe.Answer`
- Description: Responds to a teleport request

`/playerfile`
- Permission: `DeltaEss.PlayerFile`
- Description: Loads, opens, and saves a player file (for viewing or editing purposes)

`/dvanish`
- Permission: `DeltaEss.DVanish`
- Description: Marks you are vanished (for teleports and chat)

`/findplayer`
- Permissions:
  - `DeltaEss.FindPlayer` for the command 
  - `DeltaEss.FindPlayer.IgnoreVanish` to find a player even if the player is vanished
- Description: Finds if the player is online on the network and on what server

`/disposal`
- Permission: `DeltaEss.Disposal`
- Description: Opens a chest that you can use to destroy items permanently

## Other Permissions
`DeltaEss.Tp.Silent`: When you teleport to someone that does not have this permission,
 they won't get a message that you teleported to them.

`DeltaEss.SharedGameModeInv`: Your survival and creative inventories will not get switched out when you switch game modes.

`DeltaEss.GameMode.<game mode>`: If a game mode is blocked, this permission will allow you to bypass that block.

## Licence ([GPLv3](http://www.gnu.org/licenses/gpl-3.0.en.html))
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

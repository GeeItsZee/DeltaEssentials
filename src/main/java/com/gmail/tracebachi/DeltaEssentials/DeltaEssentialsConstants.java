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
package com.gmail.tracebachi.DeltaEssentials;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class DeltaEssentialsConstants
{
  public static class Channels
  {
    public static final String PLAYER_FILE_LOCK_REQUEST = "PlayerFileLockRequest";
    public static final String PLAYER_FILE_LOCK_RELEASE = "PlayerFileLockRelease";
    public static final String TELL = "Tell";
    public static final String SOCIAL_SPY = "SocialSpy";
    public static final String TP_TO = "TpTo";
    public static final String TP_HERE = "TpHere";
    public static final String TP_AFTER_LOAD = "TpAfterLoad";
    public static final String FIND_PLAYER = "FindPlayer";
  }

  public static class FormatNames
  {
    public static final String NO_PERM = "NoPerm";
    public static final String USAGE = "Usage";
    public static final String PLAYER_ONLY_COMMAND = "PlayerOnlyCommand";
    public static final String ACTION_BLOCKED = "ActionBlocked";
    public static final String SETTING_CHANGED = "SettingChanged";
    public static final String TOO_MANY_MATCHING_NAMES = "TooManyMatchingNames";
    public static final String NO_ONE_FOR_REPLY = "NoOneForReply";
    public static final String PLAYER_OFFLINE = "PlayerOffline";
    public static final String PLAYER_ONLINE = "PlayerOnline";
    public static final String BLOCKING_TELEPORTS = "BlockingTeleports";
    public static final String UNSAFE_TP_LOCATION = "UnsafeTpLocation";
    public static final String TELEPORTING_TO = "TeleportingTo";
    public static final String TELEPORTED_TO_YOU = "TeleportedToYou";
    public static final String OTHER_CANT_JOIN_PRIVATE_SERVER = "OtherCantJoinPrivateServer";

    public static final String TP_ASK_HERE_SENT = "TpAskHere/Sent";
    public static final String TP_ASK_HERE_RECEIVED = "TpAskHere/Received";
    public static final String TP_ASK_HERE_DENIED = "TpAskHere/Denied";
    public static final String TP_ASK_HERE_NO_REQUESTS = "TpAskHere/NoRequests";

    public static final String TELL_LOG = "Tell/Log";
    public static final String TELL_SENDER = "Tell/Sender";
    public static final String TELL_RECEIVER = "Tell/Receiver";
    public static final String TELL_SOCIAL_SPY = "Tell/SocialSpy";

    public static final String PLAYER_FILE_SUCCESS = "PlayerFile/Success";
    public static final String PLAYER_FILE_FAILURE = "PlayerFile/Failure";
    public static final String PLAYER_FILE_NOT_LOADED = "PlayerFile/NotLoaded";
  }

  public static class PlayerDataFileKeys
  {
    public static final String HEALTH = "Health";
    public static final String FOOD_LEVEL = "FoodLevel";
    public static final String XP_LEVEL = "XpLevel";
    public static final String XP_PROGRESS = "XpProgress";
    public static final String GAMEMODE = "Gamemode";
    public static final String EFFECTS = "Effects";
    public static final String HELD_ITEM_SLOT = "HeldItemSlot";
    public static final String SURVIVAL_INV_SECTION = "Survival";
    public static final String CREATIVE_INV_SECTION = "Creative";
    public static final String ENDER_CHEST_SECTION = "EnderChest";

    public static final String VANISHED = "DeltaEssentials.Vanished";
    public static final String BLOCKING_TELEPORTS = "DeltaEssentials.BlockingTeleports";
    public static final String SOCIAL_SPY_LEVEL = "DeltaEssentials.SocialSpyLevel";
    public static final String REPLYING_TO = "DeltaEssentials.ReplyingTo";
  }

  public static class Permissions
  {
    public static final String GAMEMODE_SHARED_INV = "DeltaEss.SharedGameModeInv";
    public static final String GAMEMODE_PERM_PREFIX = "DeltaEss.GameMode.";
    public static final String TP_IGNORE_BLOCKING_PERM = "DeltaEss.Tp.IgnoreBlocking";
    public static final String TP_IGNORE_VANISH_PERM = "DeltaEss.Tp.IgnoreVanish";
    public static final String TP_TO_PRIVATE_SERVER_PERM_PREFIX = "SockExchange.MoveTo.";
  }
}

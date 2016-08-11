package com.gmail.tracebachi.DeltaEssentials.Storage;

/**
 * Created by trace on 8/11/16.
 */
public enum SocialSpyLevel
{
    /**
     * Show all messages on all worlds.
     */
    ALL,

    /**
     * Show all messages where either the sender or receiver is on the current world.
     */
    WORLD,

    /**
     * Show no messages.
     */
    NONE
}

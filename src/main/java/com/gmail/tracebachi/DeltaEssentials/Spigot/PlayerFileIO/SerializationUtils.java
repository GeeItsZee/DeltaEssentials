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
package com.gmail.tracebachi.DeltaEssentials.Spigot.PlayerFileIO;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author GeeItsZee (tracebachi@gmail.com).
 */
class SerializationUtils
{
  private static final Pattern COMMA = Pattern.compile(",");
  private static final String ARMOR_KEY = "Armor";
  private static final String CONTENTS_KEY = "Contents";
  private static final String EXTRA_SLOTS_KEY = "ExtraSlots";

  static ConfigurationSection toConfigurationSection(ItemStack[] itemStacks)
  {
    YamlConfiguration configuration = new YamlConfiguration();

    for (int i = 0; i < itemStacks.length; ++i)
    {
      if (itemStacks[i] != null && itemStacks[i].getType() != Material.AIR)
      {
        configuration.set(Integer.toString(i), itemStacks[i]);
      }
    }

    return configuration;
  }

  static ItemStack[] toItemStackArray(ConfigurationSection section, int maxSize)
  {
    ItemStack[] destination = new ItemStack[maxSize];

    if (section != null)
    {
      for (String key : section.getKeys(false))
      {
        try
        {
          Integer keyAsInt = Integer.parseInt(key);
          if (keyAsInt >= 0 && keyAsInt < maxSize)
          {
            destination[keyAsInt] = section.getItemStack(key);
          }
        }
        catch (NumberFormatException ex)
        {
          ex.printStackTrace();
        }
      }
    }

    return destination;
  }

  static ConfigurationSection toConfigurationSection(SavedPlayerInventory savedPlayerInventory)
  {
    YamlConfiguration configuration = new YamlConfiguration();

    if (savedPlayerInventory == null)
    {
      return configuration;
    }

    ConfigurationSection serialized;
    serialized = SerializationUtils.toConfigurationSection(savedPlayerInventory.getStorage());
    configuration.set(CONTENTS_KEY, serialized);
    serialized = SerializationUtils.toConfigurationSection(savedPlayerInventory.getArmor());
    configuration.set(ARMOR_KEY, serialized);
    serialized = SerializationUtils.toConfigurationSection(savedPlayerInventory.getExtraSlots());
    configuration.set(EXTRA_SLOTS_KEY, serialized);

    return configuration;
  }

  static SavedPlayerInventory toSavedInventory(ConfigurationSection section)
  {
    if (section != null)
    {
      ItemStack[] contents = toItemStackArray(section.getConfigurationSection(CONTENTS_KEY), 36);
      ItemStack[] armor = toItemStackArray(section.getConfigurationSection(ARMOR_KEY), 4);
      ItemStack[] extraSlots = toItemStackArray(
        section.getConfigurationSection(EXTRA_SLOTS_KEY), 1);
      return new SavedPlayerInventory(contents, armor, extraSlots);
    }

    return SavedPlayerInventory.EMPTY;
  }

  static List<String> toStringList(Collection<PotionEffect> effects)
  {
    List<String> result = new ArrayList<>(effects.size());

    for (PotionEffect effect : effects)
    {
      result.add(serialize(effect));
    }

    return result;
  }

  static List<PotionEffect> toEffectList(Collection<String> serialized)
  {
    List<PotionEffect> effects = new ArrayList<>(serialized.size());

    for (String effectString : serialized)
    {
      try
      {
        PotionEffect effect = deserialize(effectString);
        effects.add(effect);
      }
      catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored)
      {
      }
    }

    return effects;
  }

  private static String serialize(PotionEffect effect)
  {
    return effect.getType().getName() + "," + effect.getAmplifier() + "," + effect.getDuration();
  }

  private static PotionEffect deserialize(String source)
  {
    String split[] = COMMA.split(source, 3);
    int amplifier = Integer.parseInt(split[1]);
    int duration = Integer.parseInt(split[2]);

    return new PotionEffect(PotionEffectType.getByName(split[0]), duration, amplifier);
  }
}

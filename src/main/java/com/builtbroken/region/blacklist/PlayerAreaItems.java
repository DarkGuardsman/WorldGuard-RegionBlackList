package com.builtbroken.region.blacklist;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Area in which the player's items have been taken and are stored in this class.
 * 
 * @author Robert Seifert
 * 
 */
public abstract class PlayerAreaItems
{
	protected final String areaName;

	protected final World world;

	protected final HashMap<Integer, ItemStack> inventory;
	protected final HashMap<Integer, ItemStack> armor;

	public PlayerAreaItems(World world, String name)
	{
		this.world = world;
		this.areaName = name;
		inventory = new LinkedHashMap<Integer, ItemStack>();
		armor = new LinkedHashMap<Integer, ItemStack>();
	}

	/** Data used to restrict the items in the player's inventory */
	public abstract List<ItemData> getInventoryData();

	/** Data used to restrict the items in the player's armor set */
	public abstract List<ItemData> getArmorData();

	/** Is our inventory data a deny list */
	public abstract boolean denyInventory();

	/** Is our inventory data a deny list */
	public abstract boolean denyArmor();

	/** Removes all banned items from the player */
	public boolean removeItems(String user)
	{
		return removeItems(getPlayer(user));
	}

	/** Returns all banned items from the player */
	public boolean returnItems(String user)
	{
		return returnItems(getPlayer(user));
	}

	/** Removes all banned items from the player */
	public boolean removeItems(Player player)
	{
		boolean taken_flag = false;

		if (takeInventory(player))
			taken_flag = true;

		if (takeArmor(player))
			taken_flag = true;

		return taken_flag;
	}

	protected boolean takeInventory(Player player)
	{
		boolean taken_flag = false;
		if (!player.hasPermission("reginv.inventory.keep"))
		{
			boolean denyList = denyInventory();
			List<ItemData> list = getInventoryData();
			if (list != null && !list.isEmpty())
			{
				for (ItemData data : list)
				{
					for (int slot = 0; slot < player.getInventory().getContents().length; slot++)
					{
						ItemStack stack = player.getInventory().getContents()[slot];
						if (stack != null)
						{
							boolean match = stack.getTypeId() == data.stack().getTypeId() && (data.allMeta() || stack.getItemMeta() == data.stack().getItemMeta());

							if (match)
							{
								if (denyList)
								{
									this.inventory.put(slot, stack);
									player.getInventory().setItem(slot, null);
									taken_flag = true;
								}
							}
							else if (!denyList)
							{
								this.inventory.put(slot, stack);
								player.getInventory().setItem(slot, null);
								taken_flag = true;
							}
						}
					}
				}
			}
		}
		return taken_flag;
	}

	protected boolean takeArmor(Player player)
	{
		boolean taken_flag = false;
		if (!player.hasPermission("reginv.armor.keep"))
		{
			boolean denyList = denyArmor();
			List<ItemData> list = getArmorData();
			ItemStack[] armorContent = player.getInventory().getArmorContents();
			if (list != null && !list.isEmpty() && armorContent != null)
			{
				for (ItemData data : list)
				{
					for (int slot = 0; slot < armorContent.length; slot++)
					{
						ItemStack stack = armorContent[slot];
						if (stack != null)
						{
							boolean match = stack.getTypeId() == data.stack().getTypeId() && (data.allMeta() || stack.getItemMeta() == data.stack().getItemMeta());

							if (match)
							{
								if (denyList)
								{
									this.armor.put(slot, stack);
									armorContent[slot] = null;
									taken_flag = true;
								}
							}
							else if (!denyList)
							{
								this.armor.put(slot, stack);
								armorContent[slot] = null;
								taken_flag = true;
							}
						}
					}
				}
			}
			player.getInventory().setArmorContents(armorContent);
		}

		return taken_flag;
	}

	/** Returns all banned items from the player */
	public boolean returnItems(Player player)
	{
		boolean return_flag = false;
		if (player != null && !this.isEmpty())
		{
			HashMap<Integer, ItemStack> updateMap = new HashMap<Integer, ItemStack>();
			Iterator<Entry<Integer, ItemStack>> it = inventory.entrySet().iterator();
			while (it.hasNext())
			{
				boolean remove = false;
				Entry<Integer, ItemStack> entry = it.next();
				if (entry.getValue() != null)
				{
					ItemStack returned = returnItem(player, entry.getKey(), entry.getValue());
					if (returned == null || returned.getAmount() <= 0)
						return_flag = remove;
					else
						updateMap.put(entry.getKey(), returned);
				}
				it.remove();
			}
			if (!updateMap.isEmpty())
				inventory.putAll(updateMap);

			// Return armor items
			updateMap = new HashMap<Integer, ItemStack>();
			it = armor.entrySet().iterator();
			ItemStack[] armorContent = player.getInventory().getArmorContents();
			while (it.hasNext())
			{
				boolean remove = false;
				Entry<Integer, ItemStack> entry = it.next();
				if (entry.getValue() != null)
				{
					System.out.println("ArmorSlot: "+entry.getKey() + "  Item: " + armorContent[entry.getKey()]);
					if (armorContent[entry.getKey()] == null || armorContent[entry.getKey()].getTypeId() == 0)
					{
						armorContent[entry.getKey()] = entry.getValue();
						return_flag = true;
					}
					else
					{
						ItemStack returned = returnItem(player, -1, entry.getValue());
						if (returned == null || returned.getAmount() <= 0)
							return_flag = remove;
						else
							updateMap.put(entry.getKey(), returned);
					}
				}
				it.remove();
			}
			player.getInventory().setArmorContents(armorContent);
			if (!updateMap.isEmpty())
				armor.putAll(updateMap);
		}
		return return_flag;
	}

	public Player getPlayer(String user)
	{
		return Bukkit.getPlayer(user);
	}

	/** Is the area's item storage empty */
	public boolean isEmpty()
	{
		return inventory.isEmpty() && armor.isEmpty();
	}

	/** Returns a single item to the player's inventory */
	public static ItemStack returnItem(Player player, ItemStack stack)
	{
		return returnItem(player, -1, stack);
	}

	/** Returns a single item to the player's inventory */
	public static ItemStack returnItem(Player player, int slot, ItemStack stack)
	{
		if (player != null && stack != null)
		{
			if (slot >= 0 && player.getInventory().getItem(slot) == null)
			{
				player.getInventory().setItem(slot, stack);
				return null;
			}
			else
			{
				HashMap<Integer, ItemStack> re = player.getInventory().addItem(stack);
				if (re != null && !re.isEmpty())
				{
					for (Entry<Integer, ItemStack> entry : re.entrySet())
					{
						if (entry.getValue() != null)
							return entry.getValue();
					}
				}
				return null;
			}
		}
		return stack;
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof String)
		{
			return ((String) object).equalsIgnoreCase(this.areaName);
		}
		return super.equals(object);
	}

	/** Name of the area */
	public String getAreaName()
	{
		return this.areaName;
	}
}
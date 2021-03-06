package com.builtbroken.region.blacklist;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemWrapper implements Externalizable
{	
	int slot = 0;
	ItemStack stack = null;

	public ItemWrapper()
	{
		
	}
	
	public ItemWrapper(ItemStack stack, int slot)
	{
		this.stack = stack;
		this.slot = slot;
	}

	public ItemStack getStack()
	{
		return stack;
	}

	public int getSlot()
	{
		return slot;
	}

	public void setStack(ItemStack stack)
	{
		this.stack = stack;
	}

	public boolean returnArmor(Player player, ItemStack[] armorContent)
	{
		if (getStack() != null)
		{
			if (armorContent[getSlot()] == null || armorContent[getSlot()].getTypeId() == 0)
			{
				armorContent[getSlot()] = getStack();
				return true;
			}
			else
			{
				return returnItem(player);
			}
		}
		return false;
	}

	/** Returns a single item to the player's inventory */
	public boolean returnItem(Player player)
	{
		ItemStack stack = returnItem(player, getSlot());
		if (stack == null || stack.getAmount() <= 0)
			return true;
		else
			this.setStack(stack);
		return false;
	}

	/** Returns a single item to the player's inventory */
	public ItemStack returnItem(Player player, int slot)
	{
		if (player != null && getStack() != null)
		{
			if (slot >= 0 && player.getInventory().getItem(slot) == null)
			{
				player.getInventory().setItem(slot, getStack());
				return null;
			}
			else
			{
				HashMap<Integer, ItemStack> re = player.getInventory().addItem(getStack());
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
		return getStack();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		slot = in.readInt();
		stack = ItemStack.deserialize((Map<String, Object>) in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeInt(slot);
		out.writeObject(stack.serialize());
	}
}

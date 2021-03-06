package com.builtbroken.region.blacklist.factions;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.builtbroken.region.blacklist.PluginRegionBlacklist;
import com.massivecraft.factions.Factions;

/**
 * Handles common factions related methods
 * 
 * @author Robert Seifert
 * 
 */
public class FactionUtility
{
	private static Factions factions = null;

	/** Gets the worldguard plugin currently loaded */
	protected static Factions factions()
	{
		if (factions == null)
		{
			Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Factions");

			if (plugin instanceof Factions)
			{
				factions = (Factions) plugin;
			}
		}
		return factions;
	}
}

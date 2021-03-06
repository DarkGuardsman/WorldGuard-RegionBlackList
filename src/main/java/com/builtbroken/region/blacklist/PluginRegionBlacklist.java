package com.builtbroken.region.blacklist;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.EventPriority;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.builtbroken.region.blacklist.factions.FactionSupport;
import com.builtbroken.region.blacklist.gp.GriefSupport;
import com.builtbroken.region.blacklist.mod.ee.EESupport;
import com.builtbroken.region.blacklist.worldguard.WorldGuardSupport;

/**
 * Bukkit plugin to work with worldguard to take as a user enters a region. Then give them back
 * after the user has left the region.
 * 
 * @since 6/24/2014
 * @author Robert Seifert
 */
public class PluginRegionBlacklist extends JavaPlugin
{
	public PluginSupport worldGuardListener;
	public PluginSupport factionsListener;
	public PluginSupport griefPreventionListener;
	public SupportHandler supportHandler;
	public Logger logger;
	public String loggerPrefix = "";
	public HashMap<String, Boolean> playerOptOutMessages = new LinkedHashMap<String, Boolean>();

	public boolean enabledMessages = true;
	public boolean enabledItemMessages = true;
	public boolean enabledWarningMessages = true;

	public String messageItemTakeTemp = "";
	public String messageItemTakeReturn = "";
	public String messageItemTakeBan = "";
	public String messageArmorTakeTemp = "";
	public String messageArmorTakeBan = "";
	public String messageArmorTakeReturn = "";

	/*
	 * TODO - list of stuff to still do
	 * 
	 * Add: Factions support
	 * 
	 * Add: Chat lang config
	 * 
	 * Add: Global item ban list
	 * 
	 * Add: Chat command to change settings
	 */

	@Override
	public void onEnable()
	{
		loggerPrefix = String.format("[InvReg %s]", this.getDescription().getVersion());
		logger().info("Enabled!");
		supportHandler = new SupportHandler(this);
		getServer().getPluginManager().registerEvents(this.supportHandler, this);

		// Plugin support loading
		loadWorldGuardSupport();
		loadFactionSupport();
		loadGriefPrevention();
		loadForgeSupport();
		supportHandler.load();

		// Config handling
		File configFile = new File(References.CONFIG);
		YamlConfiguration config = null;
		if (configFile.exists())
		{
			config = YamlConfiguration.loadConfiguration(configFile);
			supportHandler.loadConfig(config);
		}
		else
		{
			config = new YamlConfiguration();
			supportHandler.createConfig(config);
			try
			{
				config.save(configFile);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/** Loads listener that deals with Factions plugin support */
	public void loadFactionSupport()
	{
		if (factionsListener == null)
		{
			Plugin factions = getServer().getPluginManager().getPlugin("Factions");
			if (factions != null)
			{
				logger().info("Factions support loaded");
				factionsListener = new FactionSupport(this);
				supportHandler.register(factionsListener);
				getServer().getPluginManager().registerEvents(this.factionsListener, this);
			}
			else
			{
				logger().info("Factions plugin not installed! Skipping Factions support!");
			}
		}
	}

	/** Loads listener that deals with Factions plugin support */
	public void loadGriefPrevention()
	{
		if (griefPreventionListener == null)
		{
			Plugin factions = getServer().getPluginManager().getPlugin("GriefPrevention");
			if (factions != null)
			{
				logger().info("Factions support loaded");
				griefPreventionListener = new GriefSupport(this);
				supportHandler.register(griefPreventionListener);
				getServer().getPluginManager().registerEvents(this.griefPreventionListener, this);
			}
			else
			{
				logger().info("Grief Prevention plugin not installed! Skipping Grief Prevention support!");
			}
		}
	}

	/** Loads listener that deals with WorldGuard plugin support */
	public void loadWorldGuardSupport()
	{
		if (worldGuardListener == null)
		{
			Plugin wg = getServer().getPluginManager().getPlugin("WorldGuard");
			Plugin wgFlag = getServer().getPluginManager().getPlugin("WGCustomFlags");
			if (wg != null)
			{
				if (wgFlag != null)
				{
					logger().info("WorldGuard support loaded");
					worldGuardListener = new WorldGuardSupport(this);
					supportHandler.register(worldGuardListener);
					getServer().getPluginManager().registerEvents(this.worldGuardListener, this);
				}
				else
				{
					logger().info("WGCustomFlags plugin not installed! Skipping WorldGuard support!");
				}
			}
			else
			{
				logger().info("WorldGuard plugin not installed! Skipping WorldGuard support!");
			}
		}
	}

	public void loadForgeSupport()
	{
		try
		{
			Class<?> clazz = Class.forName("net.minecraftforge.event.Event");
			if (clazz != null)
			{
				logger().info("Loading Forge Mod support!");
				Field f = null;
				Event event = new Event();
				int id = 0;
				try
				{
					f = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("busID");
					f.setAccessible(true);
					id = f.getInt(MinecraftForge.EVENT_BUS);
				}
				catch (NoSuchFieldException e1)
				{
					logger().fine("Failed to get event bus ID defaulting to zero");
				}
				ForgeEventHandler handler = new ForgeEventHandler();
				event.getListenerList().register(id, EventPriority.NORMAL, handler);

				Class<?> eeclazz = Class.forName("com.pahimar.ee3.core.handlers.WorldTransmutationHandler");
				if (eeclazz != null)
				{
					logger().info("Loading EE Mod support!");
					handler.handlers.add(new EESupport(this));
				}
			}
		}
		catch (Exception e)
		{
			logger().info("Failed to load forge support");
		}
	}

	@Override
	public void onDisable()
	{
		logger().info("Disabled!");
		supportHandler.save();
	}

	/** Logger used by the plugin, mainly just prefixes everything with the name */
	public Logger logger()
	{
		if (logger == null)
		{
			logger = new Logger(PluginRegionBlacklist.this.getClass().getCanonicalName(), null)
			{
				public void log(LogRecord logRecord)
				{

					logRecord.setMessage(loggerPrefix + logRecord.getMessage());
					super.log(logRecord);
				}
			};
			logger.setParent(getLogger());
		}
		return logger;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (command.getName().equalsIgnoreCase("RegInv"))
		{
			if (args != null && args.length > 0 && args[0] != null)
			{
				String mainCmd = args[0];

				boolean isPlayer = sender instanceof Player;
				boolean subCmd_flag = args.length > 1 && args[1] != null;
				boolean subCmd2_flag = args.length > 2 && args[2] != null;
				boolean subCmd3_flag = args.length > 3 && args[3] != null;

				Player player = isPlayer ? (Player) sender : null;
				String subCmd = subCmd_flag ? args[1] : null;
				String subCmd2 = subCmd2_flag ? args[2] : null;
				String subCmd3 = subCmd3_flag ? args[3] : null;

				if (mainCmd.equalsIgnoreCase("help"))
				{
					sender.sendMessage("/RegInv version");
					if (isPlayer)
					{
						sender.sendMessage("/RegInv messages <on/off>");
					}
					return true;
				}
				else if (mainCmd.equalsIgnoreCase("version"))
				{
					sender.sendMessage("Version: " + this.getDescription().getVersion());
					return true;
				}
				else if (mainCmd.equalsIgnoreCase("messages") && isPlayer)
				{
					if (subCmd_flag)
					{
						if (subCmd.equalsIgnoreCase("on"))
						{
							playerOptOutMessages.put(player.getName(), false);
						}
						else if (subCmd.equalsIgnoreCase("off"))
						{
							playerOptOutMessages.put(player.getName(), true);
						}
					}
					else
					{
						if (playerOptOutMessages.containsKey(player.getName()))
						{
							playerOptOutMessages.put(player.getName(), !playerOptOutMessages.get(player.getName()));
						}
						else
						{
							playerOptOutMessages.put(player.getName(), true);
						}
					}
					boolean flag = playerOptOutMessages.get(player.getName());
					if (flag)
					{
						player.sendMessage("You will no longer get messages for inventory changes");
					}
					else
					{
						player.sendMessage("You will receive messages for inventory changes");
					}
					return true;
				}
				else if (mainCmd.equalsIgnoreCase("region"))
				{
					if (worldGuardListener != null)
					{
						if (subCmd_flag)
						{
							String[] newArgs = new String[args.length - 1];
							for (int i = 0; i < newArgs.length; i++)
							{
								newArgs[i] = args[i + 1];
							}
							worldGuardListener.onCommand(sender, args);
						}
						else
						{
							sender.sendMessage("Supply more args");
						}
					}
					else
					{
						sender.sendMessage("WorldGuard support was not loaded");
					}
				}
				else if (mainCmd.equalsIgnoreCase("faction"))
				{
					if (factionsListener != null)
					{
						String[] newArgs = new String[args.length - 1];
						for (int i = 0; i < newArgs.length; i++)
						{
							newArgs[i] = args[i + 1];
						}
						factionsListener.onCommand(sender, args);
					}
					else
					{
						sender.sendMessage("Factions support was not loaded");
					}
				}
			}
			sender.sendMessage("/RegInv help");
			return true;
		}
		return false;
	}

}

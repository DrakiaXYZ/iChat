package net.TheDgtl.iChat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class VariableHandler {
	private iChat ichat;
	private dYamlConfiguration newConfig;
	
	// Values loaded from config -- Global
	private HashMap<String, HashMap<String, String>> groupVars = new HashMap<String, HashMap<String, String>>();
	private HashMap<String, HashMap<String, String>> userVars = new HashMap<String, HashMap<String, String>>();
	
	// Values loaded from config -- Per World
	private HashMap<String, HashMap<String, HashMap<String, String>>> wGroupVars = new HashMap<String, HashMap<String, HashMap<String, String>>>();
	private HashMap<String, HashMap<String, HashMap<String, String>>> wUserVars = new HashMap<String, HashMap<String, HashMap<String, String>>>();
	
	// Values loaded from config -- World Variables
	private HashMap<String, HashMap<String, String>> worldVars = new HashMap<String, HashMap<String, String>>();
	
	// Dynamically assigned on connect/reload
	private HashMap<String, HashMap<String, String>> playerVars = new HashMap<String, HashMap<String, String>>();

	public VariableHandler(iChat ichat) {
		this.ichat = ichat;
		checkConfig();
		loadConfig();
	}
	
	/*
	 * Check to see if variables.yml exists
	 */
	public void checkConfig() {
		File vFile = new File(ichat.getDataFolder(), "variables.yml");
		// Variables exists, don't touch it!
		if (vFile.exists()) return;
		// Copy defaults to variables.yml
		newConfig = dYamlConfiguration.loadConfiguration(vFile);
		newConfig.options().copyDefaults(true);
		InputStream defConfigStream = iChat.class.getClassLoader().getResourceAsStream("variables.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			
			newConfig.setDefaults(defConfig);
			
			try {
				newConfig.save(vFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void loadConfig() {
		groupVars.clear();
		userVars.clear();
		playerVars.clear();
		wGroupVars.clear();
		wUserVars.clear();
		worldVars.clear();
		
		reloadConfig();
		
		// Loop through nodes, "groups" and "users" are special cases, everything else is a world
		for (String key : newConfig.getKeys(false)) {
			if (key.equals("groups")) {
				ConfigurationSection groups = newConfig.getConfigurationSection("groups");
				loadNodes(groups, groupVars);
				continue;
			}
			if (key.equals("users")) {
				ConfigurationSection users = newConfig.getConfigurationSection("users");
				loadNodes(users, userVars);
				continue;
			}
			
			// Loop through world nodes, "groups" and "users" are special cases, everything else is a world variable
			HashMap<String, String> wVars = new HashMap<String, String>();
			ConfigurationSection world = newConfig.getConfigurationSection(key);
			for (String wKey : world.getKeys(false)) {
				if (wKey.equals("groups")) {
					ConfigurationSection groups = world.getConfigurationSection("groups");
					HashMap<String, HashMap<String, String>> wGroups = new HashMap<String, HashMap<String, String>>();
					loadNodes(groups, wGroups);
					wGroupVars.put(key.toLowerCase(), wGroups);
					continue;
				}
				if (wKey.equals("users")) {
					ConfigurationSection users = world.getConfigurationSection("users");
					HashMap<String, HashMap<String, String>> wUsers = new HashMap<String, HashMap<String, String>>();
					loadNodes(users, wUsers);
					wUserVars.put(key.toLowerCase(), wUsers);
					continue;
				}
				String value = world.getString(wKey);
				if (value == null) continue;
				wVars.put(wKey.toLowerCase(), value);
			}
			worldVars.put(key.toLowerCase(), wVars);
		}
	}
	
	public void addPlayer(Player player) {
		HashMap<String, String> tmpList = new HashMap<String, String>();
		
		String group = ichat.API.getGroup(player);
		String world = player.getWorld().getName().toLowerCase();
		
		// Check if the world the player is in has variables
		HashMap<String, String> wVars = worldVars.get(world);
		if (wVars != null && !wVars.isEmpty()) {
			tmpList.putAll(wVars);
		}
		
		if (group != null && !group.isEmpty()) {
			group = group.toLowerCase();
			HashMap<String, String> gVars = groupVars.get(group);
			if (gVars != null)
				tmpList.putAll(gVars);
			
			// Check if this group has world-specific vars
			HashMap<String, HashMap<String, String>> worlds = wGroupVars.get(world);
			if (worlds != null) {
				HashMap<String, String> wgVars = worlds.get(group);
				if (wgVars != null)
					tmpList.putAll(wgVars);
			}
		}
		
		HashMap<String, String> uVars = userVars.get(player.getName().toLowerCase());
		if (uVars != null)
			tmpList.putAll(uVars);
		
		HashMap<String, HashMap<String, String>> worlds = wUserVars.get(world);
		if (worlds != null) {
			HashMap<String, String> wuVars = worlds.get(player.getName().toLowerCase());
			if (wuVars != null)
				tmpList.putAll(wuVars);
		}
		
		playerVars.put(player.getName().toLowerCase(), tmpList);
	}
	
	public void removePlayer(Player player) {
		playerVars.remove(player.getName().toLowerCase());
	}
	
	public String getKey(Player player, String key) {
		HashMap<String, String> pVars = playerVars.get(player.getName().toLowerCase());
		if (pVars == null) return "";
		String var = pVars.get(key.toLowerCase());
		if (var == null) return "";
		return var;
	}
	
	public String getKey(String group, String key) {
		HashMap<String, String> gVars = groupVars.get(group.toLowerCase());
		if (gVars == null) return "";
		String var = gVars.get(key.toLowerCase());
		if (var == null) return "";
		return var;
	}

	private void reloadConfig() {
		File vFile = new File(ichat.getDataFolder(), "variables.yml");
		if (!vFile.exists()) {
			ichat.log.info("[iChat] variables.yml does not exist. Please create it.");
			return;
		}
		newConfig = dYamlConfiguration.loadConfiguration(vFile);
	}
	
	/*
	 * Load the nodes from root into map
	 */
	private void loadNodes(ConfigurationSection root, HashMap<String, HashMap<String, String>> map) {
		for (String key : root.getKeys(false)) {
			HashMap<String, String> tmpList = new HashMap<String, String>();
			ConfigurationSection vars = root.getConfigurationSection(key);
			if (vars == null) continue;
			// Store vars
			for (String vKey : vars.getKeys(false)) {
				String value = vars.getString(vKey);
				if (value == null) continue;
				tmpList.put(vKey.toLowerCase(),  value);
			}
			map.put(key.toLowerCase(), tmpList);
		}
	}
	
	/*
	 * DEBUG
	 */
	public void debug() {
		ichat.log.info("[iChat::vh::debug]");
		// Print out all group variables
		ichat.log.info("[iChat::vh::debug] Groups");
		for (String group : groupVars.keySet()) {
			ichat.log.info("  " + group);
			HashMap<String, String> gVars = groupVars.get(group);
			for (String key : gVars.keySet()) {
				ichat.log.info("    " + key + " => " + gVars.get(key));
			}
		}
		
		// Print out all user variables
		ichat.log.info("[iChat::vh::debug] Users");
		for (String user : userVars.keySet()) {
			ichat.log.info("  " + user);
			HashMap<String, String> uVars = userVars.get(user);
			for (String key : uVars.keySet()) {
				ichat.log.info("    " + key + " => " + uVars.get(key));
			}
		}
		
		// Print out all player variables
		ichat.log.info("[iChat::vh::debug] Players");
		for (String player : playerVars.keySet()) {
			ichat.log.info("  " + player);
			HashMap<String, String> pVars = playerVars.get(player);
			for (String key : pVars.keySet()) {
				ichat.log.info("    " + key + " => " + pVars.get(key));
			}
		}
	}
}

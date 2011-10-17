package net.TheDgtl.iChat;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

public class VariableHandler {
	private iChat ichat;
	private Configuration config;
	
	// Values loaded from config
	private HashMap<String, HashMap<String, String>> groupVars = new HashMap<String, HashMap<String, String>>();
	private HashMap<String, HashMap<String, String>> userVars = new HashMap<String, HashMap<String, String>>();
	
	// Dynamically assigned on connect/reload
	private HashMap<String, HashMap<String, String>> playerVars = new HashMap<String, HashMap<String, String>>();

	public VariableHandler(iChat ichat) {
		this.ichat = ichat;
		checkConfig();
	}
	
	public void loadConfig() {
		groupVars.clear();
		userVars.clear();
		playerVars.clear();
		config.load();
		ConfigurationNode groups = config.getNode("groups");
		if (groups != null) {
			loadNodes(groups, groupVars);
		}
		
		ConfigurationNode users = config.getNode("users");
		if (users != null) {
			loadNodes(users, userVars);
		}
	}
	
	public void addPlayer(Player player) {
		HashMap<String, String> tmpList = new HashMap<String, String>();
		
		String group = ichat.API.getGroup(player);		
		if (group != null && !group.isEmpty()) {
			group = group.toLowerCase();
			if (!group.isEmpty()) {
				HashMap<String, String> gVars = groupVars.get(group);
				if (gVars != null)
					tmpList.putAll(gVars);
			}
		}
		
		HashMap<String, String> uVars = userVars.get(player.getName().toLowerCase());
		if (uVars != null)
			tmpList.putAll(uVars);
		
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

	private void checkConfig() {
		File vFile = new File(ichat.getDataFolder(), "variables.yml");
		config = new Configuration(vFile);
		if (!vFile.exists()) {
			config.setHeader(
				"# iChat Variable Config",
				"# This is now the only method for defining variables"
			);
			
			TreeMap<String, Object> defaultGroups = new TreeMap<String, Object>();
			TreeMap<String, Object> defaultGroupAdmin = new TreeMap<String, Object>();
			TreeMap<String, Object> defaultGroupDefault = new TreeMap<String, Object>();
			defaultGroupAdmin.put("prefix", "&c");
			defaultGroupAdmin.put("suffix", "");
			defaultGroupAdmin.put("name", "Admin");
			defaultGroups.put("admin", defaultGroupAdmin);
			
			defaultGroupDefault.put("prefix", "");
			defaultGroupDefault.put("suffix", "");
			defaultGroupDefault.put("name", "Guest");
			defaultGroups.put("default", defaultGroupDefault);
			config.setProperty("groups", defaultGroups);
			
			TreeMap<String, Object> defaultUsers = new TreeMap<String, Object>();
			TreeMap<String, Object> defaultUserDrakia = new TreeMap<String, Object>();
			defaultUserDrakia.put("prefix", "&e");
			defaultUsers.put("drakia", defaultUserDrakia);
			config.setProperty("users", defaultUsers);
			
			config.save();
		}
		config.load();
	}
	
	/*
	 * Load the nodes from root into map
	 */
	private void loadNodes(ConfigurationNode root, HashMap<String, HashMap<String, String>> map) {
		for (String key : root.getKeys()) {
			HashMap<String, String> tmpList = new HashMap<String, String>();
			ConfigurationNode vars = root.getNode(key);
			if (vars == null) continue;
			// Store vars
			for (String vKey : vars.getKeys()) {
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
		ichat.log.info("[iChat::ih::debug]");
		// Print out all group variables
		ichat.log.info("[iChat::ih::debug] Groups");
		for (String group : groupVars.keySet()) {
			ichat.log.info("  " + group);
			HashMap<String, String> gVars = groupVars.get(group);
			for (String key : gVars.keySet()) {
				ichat.log.info("    " + key + " => " + gVars.get(key));
			}
		}
		
		// Print out all user variables
		ichat.log.info("[iChat::ih::debug] Users");
		for (String user : userVars.keySet()) {
			ichat.log.info("  " + user);
			HashMap<String, String> uVars = userVars.get(user);
			for (String key : uVars.keySet()) {
				ichat.log.info("    " + key + " => " + uVars.get(key));
			}
		}
		
		// Print out all player variables
		ichat.log.info("[iChat::ih::debug] Players");
		for (String player : playerVars.keySet()) {
			ichat.log.info("  " + player);
			HashMap<String, String> pVars = playerVars.get(player);
			for (String key : pVars.keySet()) {
				ichat.log.info("    " + key + " => " + pVars.get(key));
			}
		}
	}
}

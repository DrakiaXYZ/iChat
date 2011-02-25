package net.TheDgtl.iChat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijikokun.bukkit.Permissions.Permissions;

public class iChat extends JavaPlugin {
	private GroupManager gm = null;
	private Permissions permissions = null;
	private double permVersion = 0;
	
	private playerListener pListener = new playerListener(this);
	
	private PluginManager pm;
	private Logger log;
	Configuration config;
	
	// Config variables
	public String censorChar = "*";
	public boolean censorColored = false;
	public String censorColor = "&f";
	public String chatColor = "&f";
	public List<String> censorWords = new ArrayList<String>();
	public String chatFormat = "[+prefix+group+suffix&f] +name: +message";
	
	public void onEnable() {
		pm = getServer().getPluginManager();
		log = getServer().getLogger();
		config = getConfiguration();
		
		if (setupPermissions()) {
			if (gm != null) log.info("[iChat] Using GroupManager for permissions");
			if (permissions != null) log.info("[iChat] Using Permissions " + permVersion + " for permissions");
		} else {
			log.info("[iChat] Permissions plugins not found, disabling plugin");
			pm.disablePlugin(this);
			return;
		}
		
		// Create default config if it doesn't exist.
		if (!(new File(getDataFolder(), "config.yml")).exists()) {
			defaultConfig();
		}
		loadConfig();
		
		// Register events
		pm.registerEvent(Event.Type.PLAYER_CHAT, pListener, Event.Priority.Normal, this);
		
		log.info(getDescription().getName() + " (v" + getDescription().getVersion() + ") enabled");
	}
	
	public void onDisable() {
		log.info("[iChat] iChat Disabled");
	}
	
	private void loadConfig() {
		config.load();
		censorChar = config.getString("censor-char", censorChar);
		censorColored = config.getBoolean("censor-colored", censorColored);
		censorColor = config.getString("censor-color", censorColor);
		chatColor = config.getString("censor-string-color", chatColor);
		censorWords = config.getStringList("censor-list", censorWords);
		chatFormat = config.getString("message-format", chatFormat);
	}
	
	private void defaultConfig() {
		config.setProperty("censor-char", censorChar);
		config.setProperty("censor-colored", censorColored);
		config.setProperty("censor-color", censorColor);
		config.setProperty("censor-string-color", chatColor);
		config.setProperty("censor-list", censorWords);
		config.setProperty("message-format", chatFormat);
		config.save();
	}

	/*
	 * Find what Permissions plugin we're using and enable it.
	 */
	private boolean setupPermissions() {
		Plugin perm;
		perm = pm.getPlugin("GroupManager");
		// We're running GroupManager
		if (perm != null) {
			if (!perm.isEnabled()) {
				pm.enablePlugin(perm);
			}
			gm = (GroupManager)perm;
			return true;
		}
		
		perm = pm.getPlugin("Permissions");
		// We're running Permissions
		if (perm != null) {
			if (!perm.isEnabled()) {
				pm.enablePlugin(perm);
			}
			permissions = (Permissions)perm;
			try {
				permVersion = Double.parseDouble(Permissions.version);
			} catch (Exception e) {
				log.info("Could not determine Permissions version: " + Permissions.version);
				return false;
			}
			return true;
		}
		// Permissions not loaded
		return false;
	}
	
	/*
	 * Parse a given text string and replace the variables/color codes.
	 */
	public String parse(String format, String[] search, String[] replace) {
		if (search.length != replace.length) return "";
		for (int i = 0; i < search.length; i++) {
			if (search[i].contains(",")) {
				for (String s : search[i].split(",")) {
					format = format.replace(s, replace[i]);
				}
			} else {
				format = format.replace(search[i], replace[i]);
			}
		}
		return format.replaceAll("(&([a-f0-9]))", "\u00A7$2");
	}
	
	/*
	 * Replace censored words.
	 */
	public String censor(String msg) {
		if (censorWords == null || censorWords.size() == 0) return msg;
		String[] split = msg.split(" ");
		StringBuilder out = new StringBuilder();
		// Loop over all words.
		for (String word : split) {
			for (String cen : censorWords) {
				if (word.equalsIgnoreCase(cen)) {
					word = star(word);
					if (censorColored) {
						word = censorColor + word + chatColor;
					}
					break;
				}
			}
			out.append(word).append(" ");
		}
		return out.toString();
	}
	private String star(String word) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < word.length(); i++)
			out.append(censorChar);
		return out.toString();
	}
	
	/*
	 * Return a health bar string.
	 */
	public String healthBar(Player player) {
		float maxHealth = 20;
		float barLength = 10;
		float health = player.getHealth();
		int fill = Math.round( (health / maxHealth) * barLength );
		String barColor = "&2";
		// 0-40: Red  40-70: Yellow  70-100: Green
		if (fill <= 4) barColor = "&4";
		else if (fill <= 7) barColor = "&e";
		else barColor = "&2";

		StringBuilder out = new StringBuilder();
		out.append(barColor);
		for (int i = 0; i < barLength; i++) {
			if (i == fill) out.append("&8");
			out.append("|");
		}
		out.append("&f");
		return out.toString();
	}
	/*
	 * Check whether the player has the given permissions.
	 */
	public boolean hasPerm(Player player, String perm, boolean def) {
		if (gm != null) {
			return gm.getPermissionHandler().has(player, perm);
		} else if (permissions != null) {
			return permissions.getHandler().has(player, perm);
		} else {
			return def;
		}
	}
	
	/*
	 * Get the players prefix. Personal prefix takes priority.
	 */
	public String getPrefix(Player player) {
		if (gm != null) {
			// This goes through the users groups looking for the variable "prefix". Returns null if doesn't exist.
			String userPrefix = gm.getHandler().getPermissionString(player.getName(), "prefix");
			return userPrefix;
		} else if (permissions != null) {
			// Check for user prefix first
			String userPrefix = permissions.getHandler().getPermissionString(player.getName(), "prefix");
			if (userPrefix != null && !userPrefix.isEmpty()) {
				return userPrefix;
			}
			// Check if the group has a prefix.
			String group = permissions.getHandler().getGroup(player.getName());
			if (group == null) return null;
			String groupPrefix = permissions.getHandler().getGroupPrefix(group);
			return groupPrefix;
		}
		log.severe("[iChat::getPrefix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get the players suffix. Personal suffix takes priority.
	 */
	public String getSuffix(Player player) {
		if (gm != null) {
			// This goes through the users groups looking for the variable "prefix". Returns null if doesn't exist.
			String userSuffix = gm.getHandler().getPermissionString(player.getName(), "suffix");
			return userSuffix;
		} else if (permissions != null) {
			// Check for user prefix first
			String userSuffix = permissions.getHandler().getPermissionString(player.getName(), "suffix");
			if (userSuffix != null && !userSuffix.isEmpty()) {
				return userSuffix;
			}
			// Check if the group has a prefix.
			String group = permissions.getHandler().getGroup(player.getName());
			if (group == null) return null;
			String groupSuffix = permissions.getHandler().getGroupSuffix(group);
			return groupSuffix;
		}
		log.severe("[iChat::getSuffix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get the players group
	 */
	public String getGroup(Player player) {
		if (gm != null) {
			String group = gm.getHandler().getGroup(player.getName());
			return group;
		} else if (permissions != null) {
			String group = permissions.getHandler().getGroup(player.getName());
			return group;
		}
		log.severe("[iChat::getGroup] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	public Logger getLog() {
		return log;
	}
}

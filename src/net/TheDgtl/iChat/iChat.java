package net.TheDgtl.iChat;

/**
 * iChat - A chat formatting plugin for Bukkit.
 * Copyright (C) 2011 Steven "Drakia" Scott <Drakia@Gmail.com>
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijikokun.bukkit.Permissions.Permissions;

public class iChat extends JavaPlugin {
	public Permissions permissions = null;
	
	private playerListener pListener = new playerListener(this);
	private final sListener serverListener = new sListener();
	
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
	public String dateFormat = "HH:mm:ss";
	
	public void onEnable() {
		pm = getServer().getPluginManager();
		log = getServer().getLogger();
		config = getConfiguration();
		
		permissions = (Permissions)checkPlugin("Permissions");
		
		// Create default config if it doesn't exist.
		if (!(new File(getDataFolder(), "config.yml")).exists()) {
			defaultConfig();
		}
		loadConfig();
		
		// Register events
		pm.registerEvent(Event.Type.PLAYER_CHAT, pListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Monitor, this);
		
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
		dateFormat = config.getString("date-format", dateFormat);
	}
	
	private void defaultConfig() {
		config.setProperty("censor-char", censorChar);
		config.setProperty("censor-colored", censorColored);
		config.setProperty("censor-color", censorColor);
		config.setProperty("censor-string-color", chatColor);
		config.setProperty("censor-list", censorWords);
		config.setProperty("message-format", chatFormat);
		config.setProperty("date-format", dateFormat);
		config.save();
	}
	
	/*
	 * Check if a plugin is loaded/enabled already. Returns the plugin if so, null otherwise
	 */
	private Plugin checkPlugin(String p) {
		Plugin plugin = pm.getPlugin(p);
		return checkPlugin(plugin);
	}
	
	private Plugin checkPlugin(Plugin plugin) {
		if (plugin != null && plugin.isEnabled()) {
			log.info("[iChat] Found " + plugin.getDescription().getName() + " (v" + plugin.getDescription().getVersion() + ")");
			return plugin;
		}
		return null;
	}
	
	/*
	 * Parse given text string for permissions variables
	 */
	public String parseVars(String format, Player p) {
		Pattern pattern = Pattern.compile("\\+\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(format);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String var = getVariable(p, matcher.group(1));
			matcher.appendReplacement(sb, Matcher.quoteReplacement(var));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
	
	/*
	 * Parse a given text string and replace the variables/color codes.
	 */
	public String parse(String format, String[] search, String[] replace) {
		if (search.length != replace.length) return "";
		for (int i = 0; i < search.length; i++) {
			if (search[i].contains(",")) {
				for (String s : search[i].split(",")) {
					if (s == null || replace[i] == null) continue;
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
	public String censor(Player p, String msg) {
		if (censorWords == null || censorWords.size() == 0) {
			if (!hasPerm(p, "ichat.color", true))
				return msg.replaceAll("(&([a-f0-9]))", "");
			else 
				return msg;
		}
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
		if (!hasPerm(p, "ichat.color", true))
			return out.toString().replaceAll("(&([a-f0-9]))", "");
		else 
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
		if (permissions != null) {
			return permissions.getHandler().has(player, perm);
		} else {
			return def;
		}
	}
	
	/*
	 * Get the players prefix. Personal prefix takes priority.
	 */
	public String getPrefix(Player player) {
		if (permissions != null) {
			// Check for user prefix first
			String userPrefix = permissions.getHandler().getUserPermissionString(player.getWorld().getName(), player.getName(), "prefix");
			if (userPrefix != null && !userPrefix.isEmpty()) {
				return userPrefix;
			}
			// Check if the group has a prefix.
			String group = permissions.getHandler().getGroup(player.getWorld().getName(), player.getName());
			if (group == null) return null;
			String groupPrefix = permissions.getHandler().getGroupPrefix(player.getWorld().getName(), group);
			return groupPrefix;
		}
		log.severe("[iChat::getPrefix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get the players suffix. Personal suffix takes priority.
	 */
	public String getSuffix(Player player) {
		if (permissions != null) {
			// Check for user prefix first
			String userSuffix = permissions.getHandler().getUserPermissionString(player.getWorld().getName(), player.getName(), "suffix");
			if (userSuffix != null && !userSuffix.isEmpty()) {
				return userSuffix;
			}
			// Check if the group has a prefix.
			String group = permissions.getHandler().getGroup(player.getWorld().getName(), player.getName());
			if (group == null) return null;
			String groupSuffix = permissions.getHandler().getGroupSuffix(player.getWorld().getName(), group);
			return groupSuffix;
		}
		log.severe("[iChat::getSuffix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get a user/group specific variable. User takes priority
	 */
	public String getVariable(Player player, String variable) {
		if (permissions != null) {
			// Check for a user variable
			String userVar = permissions.getHandler().getUserPermissionString(player.getWorld().getName(), player.getName(), variable);
			if (userVar != null && !userVar.isEmpty()) {
				return userVar;
			}
			// Check for a group variable
			String group = permissions.getHandler().getGroup(player.getWorld().getName(), player.getName());
			if (group == null) return "";
			String groupVar = permissions.getHandler().getGroupPermissionString(player.getWorld().getName(), group, variable);
			if (groupVar == null) return "";
			return groupVar;
		}
		log.severe("[iChat::getVariable] SEVERE: There is no Permissions module, why are we running?!!??!?!");
		return "";
	}
	
	/*
	 * Get the players group
	 */
	public String getGroup(Player player) {
		if (permissions != null) {
			String group = permissions.getHandler().getGroup(player.getWorld().getName(), player.getName());
			return group;
		}
		log.severe("[iChat::getGroup] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	public Logger getLog() {
		return log;
	}
	
	// Used for loading plugin dependencies
	private class sListener extends ServerListener {
		@Override
		public void onPluginEnable(PluginEnableEvent event) {
			if (permissions == null) {
				if (event.getPlugin().getDescription().getName().equalsIgnoreCase("Permissions")) {
					permissions = (Permissions)checkPlugin(event.getPlugin());
				}
			}
		}
		
		@Override
		public void onPluginDisable(PluginDisableEvent event) {
			if (event.getPlugin() == permissions) {
				log.info("[iChat] Permissions plugin lost.");
				permissions = null;
			}
		}
	}
}

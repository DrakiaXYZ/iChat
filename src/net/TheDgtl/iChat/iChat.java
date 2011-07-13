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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.command.ColouredConsoleSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
//import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijikokun.bukkit.Permissions.Permissions;

public class iChat extends JavaPlugin {
	public Permissions permissions = null;
	
	private playerListener pListener = new playerListener(this);
	//private customListener cListener = new customListener();
	
	private PluginManager pm;
	//private Logger log;
	public ColouredConsoleSender console = null;
	Configuration config;
	
	// Config variables
	public String censorChar = "*";
	public boolean censorColored = false;
	public String censorColor = "&f";
	public String chatColor = "&f";
	public List<String> censorWords = new ArrayList<String>();
	public String chatFormat = "[+prefix+group+suffix&f] +name: +message";
	public String meFormat = "* +name +message";
	public String dateFormat = "HH:mm:ss";
	
	// External interface
	public static iChat ichat = null;
	
	public void onEnable() {
		pm = getServer().getPluginManager();
		console = new ColouredConsoleSender((CraftServer)getServer());
		config = getConfiguration();
		
		permissions = (Permissions)checkPlugin("Permissions");
		
		// We now depend on Permissions, so disable here if it's not found for some reason
		if (permissions == null || !checkVersion(permissions, '3')) {
			console.sendMessage("[iChat] Permissions plugin not found or wrong version. Disabling");
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
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, pListener, Event.Priority.Normal, this);
		//pm.registerEvent(Event.Type.CUSTOM_EVENT, cListener, Event.Priority.Normal, this);
		
		// Setup external interface
		iChat.ichat = this;
		
		console.sendMessage(getDescription().getName() + " (v" + getDescription().getVersion() + ") enabled");
	}
	
	public void onDisable() {
		console.sendMessage("[iChat] iChat Disabled");
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
		meFormat = config.getString("me-format", meFormat);
	}
	
	private void defaultConfig() {
		config.setProperty("censor-char", censorChar);
		config.setProperty("censor-colored", censorColored);
		config.setProperty("censor-color", censorColor);
		config.setProperty("censor-string-color", chatColor);
		config.setProperty("censor-list", censorWords);
		config.setProperty("message-format", chatFormat);
		config.setProperty("date-format", dateFormat);
		config.setProperty("me-format", meFormat);
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
			console.sendMessage("[iChat] Found " + plugin.getDescription().getName() + " (v" + plugin.getDescription().getVersion() + ")");
			return plugin;
		}
		return null;
	}
	
	private boolean checkVersion(Plugin plugin, char Ver) {
		return (plugin.getDescription().getVersion().charAt(0) == Ver);
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
	public String replaceVars(String format, String[] search, String[] replace) {
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
			return out.toString().replaceAll("(&([a-f0-9]))", "").trim();
		else 
			return out.toString().trim();
	}
	private String star(String word) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < word.length(); i++)
			out.append(censorChar);
		return out.toString();
	}
	
	/**
	 * @param p - Player object for chatting
	 * @param msg - Message to be formatted
	 * @param chatFormat - The requested chat format string
	 * @return - New message format
	 */
	public String parseChat(Player p, String msg, String chatFormat) {
		// Variables we can use in a message
		String prefix = getPrefix(p);
		String suffix = getSuffix(p);
		String group = getGroup(p);
		if (prefix == null) prefix = "";
		if (suffix == null) suffix = "";
		if (group == null) group = "";
		String healthbar = healthBar(p);
		String health = String.valueOf(p.getHealth());
		String world = p.getWorld().getName();
		// Timestamp support
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormat);
		String time = dateFormat.format(now);
		
		// We're sending this to String.format, so we need to escape those pesky % symbols
		msg = msg.replaceAll("%", "%%");
		// Censor message
		msg = censor(p, msg);
		
		String format = parseVars(chatFormat, p);
		if (format == null) return msg;
		// Order is important, this allows us to use all variables in the suffix and prefix! But no variables in the message
		String[] search = new String[] {"+suffix,+s", "+prefix,+p", "+group,+g", "+healthbar,+hb", "+health,+h", "+world,+w", "+time,+t", "+name,+n", "+displayname,+d", "+message,+m"};
		String[] replace = new String[] { suffix, prefix, group, healthbar, health, world, time, p.getName(), p.getDisplayName(), msg };
		return replaceVars(format, search, replace);
	}
	
	/**
	 * @param p - Player object for chatting
	 * @param msg - Message to be formatted
	 * @return - New message format
	 */
	public String parseChat(Player p, String msg) {
		return parseChat(p, msg, this.chatFormat);
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
	 * Get the players group prefix.
	 */
	public String getPrefix(Player player) {
		if (permissions != null) {
			// Permissions 3 no longer has "User prefixes"
			return permissions.getHandler().getUserPrefix(player.getWorld().getName(), player.getName());
		}
		console.sendMessage("[iChat::getPrefix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get the players group suffix.
	 */
	public String getSuffix(Player player) {
		if (permissions != null) {
			// Permissions 3 no longer has "User suffixes"
			return permissions.getHandler().getUserSuffix(player.getWorld().getName(), player.getName());
		}
		console.sendMessage("[iChat::getSuffix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get a user/group specific variable. User takes priority
	 */
	public String getVariable(Player player, String variable) {
		if (permissions != null) {
			// Check for a user variable
			String userVar = permissions.getHandler().getInfoString(player.getWorld().getName(), player.getName(), variable, false);
			if (userVar != null && !userVar.isEmpty()) {
				return userVar;
			}
			// Check for a group variable
			String group = permissions.getHandler().getPrimaryGroup(player.getWorld().getName(), player.getName());
			if (group == null) return "";
			String groupVar = permissions.getHandler().getInfoString(player.getWorld().getName(), group, variable, true);
			if (groupVar == null) return "";
			return groupVar;
		}
		console.sendMessage("[iChat::getVariable] SEVERE: There is no Permissions module, why are we running?!!??!?!");
		return "";
	}
	
	/*
	 * Get the players group
	 */
	public String getGroup(Player player) {
		if (permissions != null) {
			String group = permissions.getHandler().getPrimaryGroup(player.getWorld().getName(), player.getName());
			return group;
		}
		console.sendMessage("[iChat::getGroup] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}

	/*
	private class customListener extends CustomEventListener {
		@Override
		public void onCustomEvent(Event event) {
			if (event.getEventName().equalsIgnoreCase("iChatMeEvent")) {
				console.sendMessage("iChat ME Event");
			}
		}
	}*/
	
	/*
	 * Command Handler
	 */
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("ichat")) return false;
		if (sender instanceof Player && !hasPerm((Player)sender, "ichat.reload", sender.isOp())) {
			sender.sendMessage("[iChat] Permission Denied");
			return true;
		}
		if (args.length != 1) return false;
		if (args[0].equalsIgnoreCase("reload")) {
			loadConfig();
			sender.sendMessage("[iChat] Config Reloaded");
			return true;
		}
		return false;
	}
}

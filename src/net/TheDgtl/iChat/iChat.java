package net.TheDgtl.iChat;

/**
 * iChat - A chat formatting plugin for Bukkit.
 * Copyright (C) 2011 Steven "Drakia" Scott <Drakia@Gmail.com>
 * Copyright (C) 2011 MiracleM4n <https://github.com/MiracleM4n/>
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
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
//import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

import de.bananaco.permissions.worlds.WorldPermissionsManager;

public class iChat extends JavaPlugin {
	public PluginManager pm;
	
    // Permissions
    public PermissionHandler permissions;
    public boolean permissions3;
    // bPermissions (PermissionSet)
    public WorldPermissionsManager bPermMan;
    // PermissionsBukkit (Group)
    public PermissionsPlugin pbPlug;
    // PEX (PermissionUser)
    public PermissionsEx pexPlug;

    // Vairable Handler
    public VariableHandler info;
    
    // Player Connect Time List
    public HashMap<String, Long> connectList;
	
	// Listeners
	private playerListener pListener = new playerListener(this);
	
	// Logging and Config
	public Logger log;
	Configuration config;
	
	// API
	public iChatAPI API;
	
	// Config variables
	public String iNameFormat = "[+prefix+group+suffix&f] +displayname";
	public String chatFormat = "+iname: +message";
	public String meFormat = "* +name +message";
	public String dateFormat = "HH:mm:ss";
	public boolean handleMe = true;
	
	public void onEnable() {
		API = new iChatAPI(this);
		pm = getServer().getPluginManager();
		// Workaround for pre/post 1192
		log = getServer().getLogger();
		config = new Configuration(new File(getDataFolder(), "config.yml"));
		
		setupPermissions();
		
		// Create default config if it doesn't exist.
		if (!(new File(getDataFolder(), "config.yml")).exists()) {
			defaultConfig();
		}
		loadConfig();
		
		info = new VariableHandler(this);
		info.loadConfig();
		
		connectList = new HashMap<String, Long>();
		for (Player player : getServer().getOnlinePlayers()) {
			info.addPlayer(player);
		}
		
		// Register events
		pm.registerEvent(Event.Type.PLAYER_CHAT, pListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, pListener, Event.Priority.Normal, this);
		
		log.info(getDescription().getName() + " (v" + getDescription().getVersion() + ") enabled");
	}
	
	public void onDisable() {
		log.info("[iChat] iChat Disabled");
	}
	
	private void loadConfig() {
		config.load();
		iNameFormat = config.getString("iname-format", iNameFormat);
		chatFormat = config.getString("message-format", chatFormat);
		dateFormat = config.getString("date-format", dateFormat);
		meFormat = config.getString("me-format", meFormat);
		handleMe = config.getBoolean("handle-me", handleMe);
	}
	
	private void defaultConfig() {
		config.setProperty("iname-format", iNameFormat);
		config.setProperty("message-format", chatFormat);
		config.setProperty("date-format", dateFormat);
		config.setProperty("me-format", meFormat);
		config.setProperty("handle-me", handleMe);
		config.save();
	}
	
    private void setupPermissions() {
    	// Setup already
    	if (permissions != null || bPermMan != null || pbPlug != null || pexPlug != null) return;
    	Plugin tmp = null;
    	PluginManager pm = getServer().getPluginManager();
    	
    	// Check for bPerms first
    	tmp = pm.getPlugin("bPermissions");
    	if (tmp != null && tmp.isEnabled()) {
    		log.info("[iChat] Found bPermissions v" + tmp.getDescription().getVersion());
    		bPermMan = de.bananaco.permissions.Permissions.getWorldPermissionsManager();
    		return;
    	}
    	
    	// Check for PermBukkit next
    	tmp = pm.getPlugin("PermissionsBukkit");
    	if (tmp != null) {
    		log.info("[iChat] Found PermissionsBukkit v" + tmp.getDescription().getVersion());
    		pbPlug = (PermissionsPlugin)tmp;
    		return;
    	}
    	
    	// Then PEX
    	tmp = pm.getPlugin("PermissionsEx");
    	if (tmp != null && tmp.isEnabled()) {
    		log.info("[iChat] Found PermissionsEx v" + tmp.getDescription().getVersion());
    		pexPlug = (PermissionsEx)tmp;
    		return;
    	}
    	
    	// Finally Permissions (This avoids catching bridges)
        tmp = pm.getPlugin("Permissions");
        if (tmp != null && tmp.isEnabled()) {
        	log.info("[iChat] Found Permissions v" + tmp.getDescription().getVersion());
	    	permissions = ((Permissions) tmp).getHandler();
	    	permissions3 = tmp.getDescription().getVersion().startsWith("3");
	    	return;
        }
        
    	log.info("[iChat] Permissions not found, using SuperPerms");
    	return;
    }
	
	/*
	 * Command Handler
	 */
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("ichat")) return false;
		if (sender instanceof Player && !API.checkPermissions((Player)sender, "ichat.reload")) {
			sender.sendMessage("[iChat] Permission Denied");
			return true;
		}
		if (args.length != 1) return false;
		
		if (args[0].equalsIgnoreCase("reload")) {
			loadConfig();
			info.loadConfig();
			for (Player player : getServer().getOnlinePlayers()) {
				info.addPlayer(player);
			}
			sender.sendMessage("[iChat] Config Reloaded");
			return true;
		} else if (args[0].equalsIgnoreCase("debug")) {
			info.debug();
			return true;
		}
		return false;
	}
}

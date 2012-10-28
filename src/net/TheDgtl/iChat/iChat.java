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
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import net.TheDgtl.iChat.Metrics.Metrics;
import net.krinsoft.privileges.Privileges;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

public class iChat extends JavaPlugin implements Runnable {
	public PluginManager pm;
	
	// Permission Handler Name
	private String permHandler = "";
    // Permissions
    public PermissionHandler permissions;
    public boolean permissions3;
    // bPermissions (PermissionSet)
    public Plugin bPerm;
    // PermissionsBukkit (Group)
    public PermissionsPlugin pbPlug;
    // PEX (PermissionUser)
    public PermissionsEx pexPlug;
    // GroupManader
    public GroupManager gMan;
    // Privileges
    public Privileges priv;

    // Vairable Handler
    public VariableHandler info;
    
    // Player Connect Time List
    public HashMap<String, Long> connectList;
	
	// Logging and Config
	public Logger log;
	FileConfiguration newConfig;
	
	// API
	public iChatAPI API;
	
	// Config variables
	public String iNameFormat = "[+prefix+group+suffix&f] +displayname";
	public String chatFormat = "+iname: +message";
	public String meFormat = "* +name +message";
	public String dateFormat = "HH:mm:ss";
	public Integer timeOffset = null;
	public boolean handleMe = true;
	public boolean mePerm = false;
	public int refreshTimeout = 100;
	
	public void onEnable() {
		API = new iChatAPI(this);
		pm = getServer().getPluginManager();
		log = getServer().getLogger();
		newConfig = this.getConfig();
		
		setupPermissions();
		loadConfig();
		
		info = new VariableHandler(this);
		connectList = new HashMap<String, Long>();
		
		// Register events
		pm.registerEvents(new playerListener(this), this);
		
		// Create the task for refreshing user data
		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 0, refreshTimeout);
		
		log.info(getDescription().getName() + " (v" + getDescription().getVersion() + ") enabled");
		
		// Enable MCStats Metrics
		try {
			Metrics metrics = new Metrics(this);
			
			// Setup a graph for keeping track of Permission handler user
			Metrics.Graph phGraph = metrics.createGraph("Permission Handler");
			phGraph.addPlotter(new Metrics.Plotter(permHandler) {
				@Override
				public int getValue() {
					return 1;
				}
			});
			metrics.start();
		} catch (IOException ex) {
			// Something went wrong. Owell.
		}
	}
	
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		log.info("[iChat] iChat Disabled");
	}
	
	private void loadConfig() {
		reloadConfig();
		newConfig = this.getConfig();
		newConfig.options().copyDefaults(true);
		iNameFormat = newConfig.getString("iname-format");
		chatFormat = newConfig.getString("message-format");
		dateFormat = newConfig.getString("date-format");
		meFormat = newConfig.getString("me-format");
		handleMe = newConfig.getBoolean("handle-me");
		mePerm = newConfig.getBoolean("me-permissions");
		refreshTimeout = newConfig.getInt("variable-refresh");
		// Only get the timezone if it's specified
		if (newConfig.isSet("time-offset"))
			timeOffset = newConfig.getInt("time-offset");
		
		saveConfig();
		
		// Restart update task
		getServer().getScheduler().cancelTasks(this);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 0, refreshTimeout);
	}
	
    private void setupPermissions() {
    	// Setup already
    	if (permissions != null || bPerm != null || pbPlug != null || pexPlug != null || gMan != null || priv != null) return;
    	Plugin tmp = null;
    	PluginManager pm = getServer().getPluginManager();
    	
    	// Check for bPerms first
    	tmp = pm.getPlugin("bPermissions");
    	if (tmp != null && tmp.isEnabled()) {
    		permHandler = "bPermissions";
    		log.info("[iChat] Found bPermissions v" + tmp.getDescription().getVersion());
    		bPerm = tmp;
    		return;
    	}
    	
    	// Check for PermBukkit next
    	tmp = pm.getPlugin("PermissionsBukkit");
    	if (tmp != null) {
    		permHandler = "PermissionsBukkit";
    		log.info("[iChat] Found PermissionsBukkit v" + tmp.getDescription().getVersion());
    		pbPlug = (PermissionsPlugin)tmp;
    		return;
    	}
    	
    	// Then PEX
    	tmp = pm.getPlugin("PermissionsEx");
    	if (tmp != null && tmp.isEnabled()) {
    		permHandler = "PermissionsEx";
    		log.info("[iChat] Found PermissionsEx v" + tmp.getDescription().getVersion());
    		pexPlug = (PermissionsEx)tmp;
    		return;
    	}
    	
    	// Then GroupManager
    	tmp = pm.getPlugin("GroupManager");
    	if (tmp != null && tmp.isEnabled()) {
    		permHandler = "GroupManager";
    		log.info("[iChat] Found GroupManager v" + tmp.getDescription().getVersion());
    		gMan = (GroupManager)tmp;
    		return;
    	}
    	
    	// Then privileges
    	tmp = pm.getPlugin("Privileges");
    	if (tmp != null && tmp.isEnabled()) {
    		permHandler = "Privileges";
    		log.info("[iChat] Found Privileges v" + tmp.getDescription().getVersion());
    		priv = (Privileges)tmp;
    		return;
    	}
    	
    	// Finally Permissions (This avoids catching bridges)
        tmp = pm.getPlugin("Permissions");
        if (tmp != null && tmp.isEnabled()) {
        	permHandler = "Permissions";
        	log.info("[iChat] Found Permissions v" + tmp.getDescription().getVersion());
	    	permissions = ((Permissions) tmp).getHandler();
	    	permissions3 = tmp.getDescription().getVersion().startsWith("3");
	    	if (permissions3)
	    		permHandler = "Permissions3";
	    	return;
        }
        
        permHandler = "SuperPerms";
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
			sender.sendMessage("[iChat] Config Reloaded");
			return true;
		} else if (args[0].equalsIgnoreCase("debug")) {
			info.debug();
			
			// Print config informations
			log.info("[ichat::debug]");
			log.info("iNameFormat = " + newConfig.getString("iname-format"));
			log.info("chatFormat = " + newConfig.getString("message-format"));
			log.info("dateFormat = " + newConfig.getString("date-format"));
			log.info("meFormat = " + newConfig.getString("me-format"));
			log.info("handleMe = " + newConfig.getBoolean("handle-me"));
			log.info("mePerm = " + newConfig.getBoolean("me-permissions"));
			
			log.info("plugins/iChat/config.yml exists: " + (new File(this.getDataFolder(), "config.yml")).exists());
			log.info("plugins/iChat/variables.yml exists: " + (new File(this.getDataFolder(), "variables.yml")).exists());
			return true;
		}
		return false;
	}
	
	// Reload player data every x seconds (Default 5)
	@Override
	public void run() {
		for (final Player p : getServer().getOnlinePlayers()) {
			info.addPlayer(p);
		}
	}
}
